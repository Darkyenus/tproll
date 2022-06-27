package unit;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.SimpleLogFunction;
import com.darkyen.tproll.logfunctions.adapters.ThreadedSafetyNet;
import com.vmlens.api.AllInterleavings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadedSafetyNetTest {

    public static long logAccumulator = 1;

    private double test(int threadCount, int messagesPerThread, int threadWork, int logWork, boolean useSafetyNet) throws InterruptedException {
        final long startTime = System.nanoTime();
        final AtomicInteger logCount = new AtomicInteger(0);
        final LogFunction parent = new LogFunction() {
            @Override
            public void log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
                if (name.equals("test")) {
                    synchronized(logCount) {
                        logCount.incrementAndGet();

                        for (int w = 0; w < logWork; w++) {
                            logAccumulator = (long) (logAccumulator * Math.sin(w * 1.23456)) + 1;
                        }
                    }
                }
            }
        };

        ThreadedSafetyNet safetyNet = null;
        if (useSafetyNet) {
            safetyNet = new ThreadedSafetyNet(parent, 256, -1L, -1L);
            TPLogger.setLogFunction(safetyNet);
        } else {
            TPLogger.setLogFunction(parent);
        }

        final Thread[] threads = new Thread[threadCount];
        final Logger LOG = LoggerFactory.getLogger("test");
        final AtomicLong maxWait = new AtomicLong();
        for (int i = 0; i < threadCount; i++) {
            final int I = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    long workAccumulator = 1;
                    for (int w = 0; w < threadWork; w++) {
                        workAccumulator = (long) (workAccumulator * Math.sin(w * 1.23456)) + 1;
                    }
                    final long logStart = System.nanoTime();
                    LOG.info("message {}:{}", I, workAccumulator == 0 ? -j : j);
                    final long logWait = System.nanoTime() - logStart;
                    while (true) {
                        final long currentMaxWait = maxWait.get();
                        if (logWait <= currentMaxWait) {
                            break;
                        }
                        if (maxWait.compareAndSet(currentMaxWait, logWait)) {
                            break;
                        }
                    }
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }

        TPLogger.setLogFunction(SimpleLogFunction.CONSOLE_LOG_FUNCTION);

        Assert.assertEquals(threadCount * messagesPerThread, logCount.get());
        double nsPerMessage = (double) (System.nanoTime() - startTime) / (messagesPerThread * threadCount);
        System.out.println("test("+threadCount+", "+messagesPerThread+", "+threadWork+", "+logWork+", "+useSafetyNet+")");
        System.out.printf("    ns/KM: %9.5f     maxWait: %9d ns\n", nsPerMessage, maxWait.get());
        return nsPerMessage;
    }

    @Ignore("does not work")
    @Test
    public void testVMLens() throws InterruptedException {
        int interleavings = 0;
        try (AllInterleavings allInterleavings = AllInterleavings.builder("ThreadedSafetyNet").maximumRuns(Integer.MAX_VALUE)
                .showStatementsInExecutor()
                .showStatementsWhenSingleThreaded()
                .showNonVolatileSharedMemoryAccess()
                .maximumSynchronizationActionsPerThread(Integer.MAX_VALUE)
                .build()) {
            while (allInterleavings.hasNext()) {
                interleavings++;
                test(50, 50, 0, 0, true);
            }
        }
        System.out.println("testVMLens interleavings: "+interleavings);
    }

    @Test
    public void testBrute() throws InterruptedException {
        test(10, 10000, 1, 100, true);
        test(10, 10000, 1, 100, false);
    }

    @Test
    public void collapse() {
        final StringBuilder result = new StringBuilder();
        final LogFunction parent = new LogFunction() {
            @Override
            public void log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
                if (name.length() == 1) {
                    synchronized(result) {
                        result.append(name);
                    }
                }
            }
        };
        final ThreadedSafetyNet net = new ThreadedSafetyNet(parent, 100, -1, 100);
        TPLogger.setLogFunction(net);


        final Logger a = LoggerFactory.getLogger("a");
        final Logger b = LoggerFactory.getLogger("b");
        final Logger c = LoggerFactory.getLogger("c");

        a.info(".");//a
        a.info(".");
        a.info(".");
        a.info(".");
        b.info(".");//b
        b.info(".");
        c.info(".");//c
        b.info(".");//b
        a.info(".");//a
        b.info(".");//b
        b.info(".");

        TPLogger.setLogFunction(SimpleLogFunction.CONSOLE_LOG_FUNCTION);

        Assert.assertEquals(result.toString(), "abcbab");
    }

}
