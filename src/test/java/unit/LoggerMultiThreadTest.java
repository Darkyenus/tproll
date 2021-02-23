package unit;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.TPLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Marker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test for TPLogger
 */
public class LoggerMultiThreadTest {

    private static final String LOGGER_NAME = "TEST-LOGGER";
    private static final Object LOGGER_LOCK = new Object();

    @Test
    public void stressTest() throws InterruptedException {
        final TPLogger log = new TPLoggerFactory().getLogger(LOGGER_NAME);
        final StringBuilder logOut = new StringBuilder();
        TPLogger.setLogFunction(new LogFunction() {
            @Override
            public void log(@NotNull String name, long time, byte level, Marker marker, @NotNull CharSequence content) {
                synchronized (LOGGER_LOCK) {
                    logOut.append(content);
                }
            }
        });

        final int threads = 10;
        final int messagesPerThread = 1000;
        final String message = "This is a quite long message which does not interfere with regex ";
        final CountDownLatch countDownLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread("StressTest "+i){
                @Override
                public void run() {
                    for (int j = 0; j < messagesPerThread; j++) {
                        log.info(message);
                    }
                    countDownLatch.countDown();
                }
            }.start();
        }

        countDownLatch.await(10, TimeUnit.SECONDS);
        final int expectedRepeats = threads * messagesPerThread;

        int index = 0;
        int matches = 0;
        while (index < logOut.length()) {
            for (int i = 0; i < message.length(); i++, index++) {
                final char is = logOut.charAt(index);
                final char shouldBe = message.charAt(i);

                Assert.assertEquals("Problem at index "+index+" after matching "+matches+" message instances", shouldBe, is);
            }
            matches++;
        }

        Assert.assertEquals("Message repeats", expectedRepeats, matches);
    }
}
