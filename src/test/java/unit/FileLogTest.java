package unit;

import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.AbstractAdapterLogFunction;
import com.darkyen.tproll.logfunctions.DateTimeFileCreationStrategy;
import com.darkyen.tproll.logfunctions.FileLogFunction;
import com.darkyen.tproll.logfunctions.LogFileHandler;
import com.darkyen.tproll.logfunctions.SimpleLogFunction;
import com.darkyen.tproll.util.TimeFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;

import static com.darkyen.tproll.logfunctions.DateTimeFileCreationStrategy.FOLDER_SIZE_LIMIT_NONE;

public class FileLogTest {

    final File logDir = new File("test logs").getAbsoluteFile();

    final Logger LOG = LoggerFactory.getLogger("TEST");

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void prepareLogDir() {
        logDir.mkdirs();
        final File[] files = logDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        final File[] newList = logDir.listFiles();
        Assume.assumeTrue(newList == null || newList.length == 0);
    }

    @Test
    public void remainingSpaceTest() {
        final long bytesToLog = 100_000_000;//100MB
        final long freeSpace = logDir.getFreeSpace();
        Assume.assumeTrue("Not enough free space to test logging in constrained space conditions (ironic huh)", freeSpace > bytesToLog * 2);

        TPLogger.setLogFunction(new AbstractAdapterLogFunction(new FileLogFunction(
                new TimeFormatter.RelativeTimeFormatter(false, false, false, true, true),
                new LogFileHandler(logDir, new DateTimeFileCreationStrategy(
                        DateTimeFileCreationStrategy.DEFAULT_DATE_FILE_NAME_FORMATTER,
                        false,
                        DateTimeFileCreationStrategy.DEFAULT_LOG_FILE_EXTENSION,
                        512 * 1000,
                        Duration.ofDays(60)), false,
                        freeSpace - bytesToLog, Long.MAX_VALUE, true))) {
            @Override
            public boolean log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
                super.log(name, time, level, marker, content);
                return true;// Otherwise it goes to stderr, which we don't want
            }
        });

        final String logMessage = "This is just some message that is somewhat long. There are longer, but we can just loop more.";
        final int loopTimes = (int) (bytesToLog / logMessage.length() * 2);
        System.out.println("Will loop "+loopTimes+" times");


        int percent = 0;
        for (int i = 0; i < loopTimes; i++) {
            final int p = i * 100 / loopTimes;
            if (p > percent) {
                percent = p;
                System.err.println(p+"%");
            }
            LOG.info(logMessage);
        }
        TPLogger.setLogFunction(SimpleLogFunction.CONSOLE_LOG_FUNCTION);

        final long freeSpaceEnd = logDir.getFreeSpace();
        final long expectedFreeSpace = freeSpace - bytesToLog - bytesToLog / 5 /*some reserve for ~1MB overshoot and unrelated system activity*/;

        Assert.assertTrue("Expected "+expectedFreeSpace+" free bytes, got "+freeSpaceEnd, freeSpaceEnd >= expectedFreeSpace);
    }

    @Test
    public void maxFileSize() {
        TPLogger.setLogFunction(new FileLogFunction(
                new TimeFormatter.RelativeTimeFormatter(false, false, false, true, true),
                new LogFileHandler(logDir, new DateTimeFileCreationStrategy(
                        DateTimeFileCreationStrategy.DEFAULT_DATE_FILE_NAME_FORMATTER,
                        true,
                        DateTimeFileCreationStrategy.DEFAULT_LOG_FILE_EXTENSION,
                        512 * 1000,
                        Duration.ofDays(60)), false,
                        1, 1000, true)));

        final StringBuilder kbMessageBuilder = new StringBuilder();
        for (int i = 0; kbMessageBuilder.length() < 1000; i++) {
            kbMessageBuilder.append(i);
        }
        final String kbMessage = kbMessageBuilder.toString();

        for (int i = 0; i < 3; i++) {
            LOG.info("{} - {}", i, kbMessage);
        }

        TPLogger.setLogFunction(SimpleLogFunction.CONSOLE_LOG_FUNCTION);

        final File[] files = logDir.listFiles();
        Assert.assertNotNull(files);
        Assert.assertEquals(3, files.length);
    }

    @Test
    public void compression() {
        TPLogger.setLogFunction(new FileLogFunction(
                new TimeFormatter.RelativeTimeFormatter(false, false, false, true, true),
                new LogFileHandler(logDir, new DateTimeFileCreationStrategy(
                        DateTimeFileCreationStrategy.DEFAULT_DATE_FILE_NAME_FORMATTER,
                        true,
                        DateTimeFileCreationStrategy.DEFAULT_LOG_FILE_EXTENSION,
                        512 * 1000,
                        Duration.ofDays(60)), true,
                        1, 1000, true)));

        final StringBuilder kbMessageBuilder = new StringBuilder();
        for (int i = 0; kbMessageBuilder.length() < 1000; i++) {
            kbMessageBuilder.append(i);
        }
        final String kbMessage = kbMessageBuilder.toString();

        for (int i = 0; i < 3; i++) {
            LOG.info("{} - {}", i, kbMessage);
        }

        TPLogger.setLogFunction(SimpleLogFunction.CONSOLE_LOG_FUNCTION);

        final File[] files = logDir.listFiles();
        Assert.assertNotNull(files);
        Assert.assertEquals(Arrays.toString(files), 3, files.length);

        for (File file : files) {
            Assert.assertTrue(file.getName()+" ends with .gz", file.getName().endsWith(".gz"));
        }
    }

    private static CharSequence generateMessage(int bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("MSG(").append(bytes).append(")=");
        final String cycle = "0123456789ABCDEFGHIJKLMNOPQRTUVWXYZ";
        for (int i = 0; i < bytes; i++) {
            sb.append(cycle.charAt(i % cycle.length()));
        }
        return sb;
    }

    @Test
    public void hugeMessages() throws IOException {
        final int steps = 1000;
        final int byteIncrement = 100;

        for (File file : logDir.listFiles()) {
            file.delete();
        }

        TPLogger.setLogFunction(new FileLogFunction(new TimeFormatter.RelativeTimeFormatter(false, false, false, false, true),
                new LogFileHandler(
                        logDir,
                        new DateTimeFileCreationStrategy(
                                DateTimeFileCreationStrategy.DEFAULT_DATE_FILE_NAME_FORMATTER,
                                false,
                                DateTimeFileCreationStrategy.DEFAULT_LOG_FILE_EXTENSION,
                                FOLDER_SIZE_LIMIT_NONE,
                                Duration.ofDays(60)),
                        false, 500_000_000/*500MB*/, 500_000_000/*500MB*/, false)));

        for (int i = 0; i < steps; i++) {
            LOG.info("{}", generateMessage(i * byteIncrement));
        }

        TPLogger.setLogFunction(SimpleLogFunction.CONSOLE_LOG_FUNCTION);

        StringBuilder allLoggedData = new StringBuilder();
        for (File file : logDir.listFiles()) {
            allLoggedData.append(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        }

        int index = 0;
        for (int i = 0; i < steps; i++) {
            String msg = generateMessage(i * byteIncrement).toString();
            int foundAt = allLoggedData.indexOf(msg, index);
            Assert.assertNotEquals("Message "+i+" not found", -1, foundAt);
            index = foundAt + msg.length();
        }
    }
}
