package com.darkyen.tproll.logfunctions.adapters;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.AbstractAdapterLogFunction;
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
public final class ThreadedSafetyNet extends AbstractAdapterLogFunction {

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
        super(parent);
        this.queue = new ArrayBlockingQueue<>(capacity, false);
        this.maxWaitUntilDropMs = maxWaitUntilDropMs;
        this.deduplicationMs = deduplicationMs;
    }

    @Override
    public boolean log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
        final MessageData data = new MessageData();
        data.name = name;
        data.time = time;
        data.level = level;
        data.marker = marker;
        data.content.append(content);

        try {
            if (maxWaitUntilDropMs < 0) {
                queue.put(data);
                return true;
            } else {
                if (queue.offer(data, maxWaitUntilDropMs, TimeUnit.MILLISECONDS)) {
                    return true;
                }
            }
        } catch (InterruptedException ignored) {}

        dropped.incrementAndGet();
        return false;
    }

    private volatile LogThread logThread = null;

    private final class LogThread extends Thread {

        private static final int STATE_RUNNING = 0;
        private static final int STATE_BLOCKING = 1;
        private static final int STATE_RUNNING_UNTIL_EMPTY = 2;
        private static final int STATE_STOP = 3;
        public final AtomicInteger state = new AtomicInteger(STATE_RUNNING);

        public LogThread() {
            super("ThreadedSafetyNet");
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
                            boolean noWait = state == STATE_RUNNING_UNTIL_EMPTY;
                            while (true) {
                                try {
                                    nextData = noWait ? queue.poll() : queue.poll(timeout - System.nanoTime(), TimeUnit.NANOSECONDS);
                                    if (nextData == null) {
                                        break;
                                    }
                                } catch (InterruptedException e) {
                                    noWait = true;
                                    continue;
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

    @Override
    public void start() {
        super.start();
        (this.logThread = new LogThread()).start();
    }

    @Override
    public void stop() {
        super.stop();
        final LogThread thread = this.logThread;
        thread.stopAndJoin(true, true);
        this.logThread = null;
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
