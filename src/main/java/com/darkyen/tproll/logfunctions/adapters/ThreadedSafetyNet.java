package com.darkyen.tproll.logfunctions.adapters;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Marker;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects messages from multiple threads and serializes them onto one thread,
 * without blocking the original threads.
 *
 * It also collapses duplicate messages.
 */
public class ThreadedSafetyNet extends LogFunction {

    private final LogFunction parent;
    private final long maxWaitUntilDropMs;
    private final long deduplicationMs;

    private final AtomicInteger dropped = new AtomicInteger(0);

    private final ArrayBlockingQueue<MessageData> queue;

    /**
     * @param parent logger to log into
     * @param capacity of the internal message buffer, greater capacity uses more memory but handles longer bursts without blocking or dropping
     * @param maxWaitUntilDropMs for how many milliseconds should the log function block when the buffer is full before dropping the message. Set to -1 to never drop messages (and potentially block forever) or to 0 to never ever wait, even if it means that messages over capacity will be lost.
     * @param deduplicationMs sequential identical messages (identical in all but time) that arrive within this many milliseconds will be collapsed into one message. -1 to disable collapsing.
     */
    public ThreadedSafetyNet(LogFunction parent, int capacity, long maxWaitUntilDropMs, long deduplicationMs) {
        this.parent = parent;
        this.queue = new ArrayBlockingQueue<>(capacity, false);
        this.maxWaitUntilDropMs = maxWaitUntilDropMs;
        this.deduplicationMs = deduplicationMs;
    }

    private synchronized void ensureStarted() {
        if (logThread != null) {
            return;
        }
        startLogThread();
        startedAutomatically = true;

        Runtime.getRuntime().addShutdownHook(new Thread("ThreadedSafetyNetShutdownHook") {
            @Override
            public void run() {
                synchronized (ThreadedSafetyNet.this) {
                    startedAutomatically = false;
                    stopLogThread(true, true);
                }
            }
        });
    }

    @Override
    public void log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
        if (logThread == null) {
            ensureStarted();
        }

        final MessageData data = new MessageData();
        data.name = name;
        data.time = time;
        data.level = level;
        data.marker = marker;
        data.content.append(content);

        try {
            if (maxWaitUntilDropMs < 0) {
                queue.put(data);
                return;
            } else {
                if (queue.offer(data, maxWaitUntilDropMs, TimeUnit.MILLISECONDS)) {
                    return;
                }
            }
        } catch (InterruptedException ignored) {}

        dropped.incrementAndGet();
    }

    private boolean started = false;
    private boolean startedAutomatically = false;
    private volatile LogThread logThread = null;

    private final class LogThread extends Thread {

        private LogThread oldLogThread;
        private static final int STATE_RUNNING = 0;
        private static final int STATE_BLOCKING = 1;
        private static final int STATE_RUNNING_UNTIL_EMPTY = 2;
        private static final int STATE_STOP = 3;
        public final AtomicInteger state = new AtomicInteger(STATE_RUNNING);

        public LogThread(LogThread logThread) {
            super("ThreadedSafetyNet");
            this.oldLogThread = logThread;
            setPriority(Thread.MIN_PRIORITY + 2);
            setDaemon(true);
        }

        public void stopAndJoin(boolean runUntilEmpty, boolean join) {
            final int newState = runUntilEmpty ? STATE_RUNNING_UNTIL_EMPTY : STATE_STOP;
            while (true) {
                final int state = this.state.get();
                if (state == STATE_RUNNING) {
                    if (this.state.compareAndSet(state, newState)) {
                        break;
                    }
                } else if (state == STATE_BLOCKING) {
                    if (this.state.compareAndSet(state, newState)) {
                        this.interrupt();
                        break;
                    }
                } else {
                    // Looks like it has already been stopped
                    break;
                }
            }

            if (join) {
                try {
                    this.join();
                } catch (InterruptedException ignored) {
                }
            }
        }

        @Override
        public void run() {
            final LogThread oldThread = this.oldLogThread;
            if (oldThread != null) {
                this.oldLogThread = null;
                oldThread.stopAndJoin(false, true);
            }

            MessageData nextData = null;
            final LogFunction parent = ThreadedSafetyNet.this.parent;
            final ArrayBlockingQueue<MessageData> queue = ThreadedSafetyNet.this.queue;
            while (true) {
                int state = this.state.get();
                final MessageData messageData;
                if (nextData != null) {
                    messageData = nextData;
                    nextData = null;
                } else if (state == STATE_STOP) {
                    return;
                } else if (state == STATE_RUNNING_UNTIL_EMPTY) {
                    messageData = queue.poll();
                    if (messageData == null) {
                        return;
                    }
                } else {
                    assert state == STATE_RUNNING;
                    if (!this.state.compareAndSet(state, STATE_BLOCKING)) {
                        continue;// State has just changed! Are we are being shut-down?
                    }
                    // Now we are open to interruptions
                    try {
                        try {
                            messageData = queue.take();
                        } catch (InterruptedException e) {
                            // Just as we suspected, we are being shut down.
                            // Just in case it is an error, cycle again.
                            continue;
                        }
                    } finally {
                        // If we are still running normally, switch it back.
                        // If we were interrupted in meantime, it will be handled later, we have already popped the message and we have to use it.
                        this.state.compareAndSet(STATE_BLOCKING, STATE_RUNNING);
                    }
                }

                // Deduplication
                int repeats = 0;
                long lastRepeatTime = 0;
                // Simple non-blocking deduplication
                if (deduplicationMs == 0L) {
                    while (true) {
                        nextData = queue.poll();
                        if (nextData == null) {
                            break;
                        }

                        if (messageData.matches(nextData)) {
                            repeats++;
                            lastRepeatTime = nextData.time;
                        } else {
                            break;
                        }
                    }
                } else if (deduplicationMs > 0) {
                    // Blocking deduplication
                    state = this.state.get();
                    if ((state == STATE_RUNNING || state == STATE_RUNNING_UNTIL_EMPTY) && this.state.compareAndSet(state, STATE_BLOCKING)) {
                        try {
                            final long timeout = System.nanoTime() + deduplicationMs * 1000000;
                            while (true) {
                                try {
                                    nextData = queue.poll(timeout - System.nanoTime(), TimeUnit.NANOSECONDS);
                                    if (nextData == null) {
                                        break;
                                    }
                                } catch (InterruptedException e) {
                                    nextData = null;
                                    break;
                                }

                                if (messageData.matches(nextData)) {
                                    repeats++;
                                    lastRepeatTime = nextData.time;
                                } else {
                                    break;
                                }
                            }

                        } finally {
                            this.state.compareAndSet(STATE_BLOCKING, state);
                        }
                    }
                }

                if (repeats > 0) {
                    messageData.content.append("\n(repeated ").append(repeats).append(" times over ").append(lastRepeatTime - messageData.time).append(" ms)");
                }

                parent.log(messageData.name, messageData.time, messageData.level, messageData.marker, messageData.content);

                final int dropped = ThreadedSafetyNet.this.dropped.getAndSet(0);
                if (dropped > 0) {
                    parent.log("ThreadedSafetyNet", System.currentTimeMillis(), TPLogger.LOG, null, "Dropped "+dropped+" log(s)");
                }
            }
        }
    }

    /**
     * Manually starts the log thread.
     * There is no need to call it, but when you do, call {@link #stopLogThread} when you are done.
     * If first log message arrives before this call, the log thread will start and stop automatically,
     * in which case this cannot be called again on this instance.
     */
    public synchronized void startLogThread() {
        if (startedAutomatically) {
            throw new IllegalStateException("Log thread already started automatically");
        }
        if (started) {
            throw new IllegalStateException("Log thread is already running");
        }
        started = true;
        (this.logThread = new LogThread(this.logThread)).start();
    }

    public synchronized void stopLogThread(boolean runUntilEmpty, boolean join) {
        if (startedAutomatically) {
            throw new IllegalStateException("Can't stop automatically started thread");
        }
        if (!started) {
            throw new IllegalStateException("Log thread is already running");
        }
        started = false;
        final LogThread thread = this.logThread;
        thread.stopAndJoin(runUntilEmpty, join);
    }

    /**
     * This call is not serialized and is passed through to parent directly.
     */
    @Override
    public boolean isEnabled(byte level, @Nullable Marker marker) {
        return parent.isEnabled(level, marker);
    }

    private static final class MessageData {
        @NotNull String name = "";
        long time = 0L;
        byte level = 0;
        @Nullable Marker marker = null;
        final StringBuilder content = new StringBuilder();

        boolean matches(MessageData other) {
            return name.equals(other.name) && level == other.level && Objects.equals(marker, other.marker) && contentEquals(content, other.content);
        }
    }

    private static boolean contentEquals(@NotNull CharSequence cs1, @NotNull CharSequence cs2) {
        final int l1 = cs1.length();
        final int l2 = cs2.length();
        if (l1 != l2) {
            return false;
        }

        for (int i = 0; i < l1; i++) {
            final char c1 = cs1.charAt(i);
            final char c2 = cs2.charAt(i);
            if (c1 != c2) {
                return false;
            }
        }

        return true;
    }
}
