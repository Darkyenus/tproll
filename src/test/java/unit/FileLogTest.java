package unit;

import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.DateTimeFileCreationStrategy;
import com.darkyen.tproll.logfunctions.FileLogFunction;
import com.darkyen.tproll.logfunctions.LogFileCreationStrategy;
import com.darkyen.tproll.logfunctions.LogFileHandler;
import com.darkyen.tproll.logfunctions.SimpleLogFunction;
import com.darkyen.tproll.util.TimeFormatter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;

public class FileLogTest {

    @Test
    public void remainingSpaceTest() {

        final File logDir = new File("test logs").getAbsoluteFile();
        logDir.mkdirs();
        final long bytesToLog = 100_000_000;//100MB
        final long freeSpace = logDir.getFreeSpace();
        Assume.assumeTrue("Not enough free space to test logging in constrained space conditions (ironic huh)", freeSpace > bytesToLog * 2);

        TPLogger.setLogFunction(new FileLogFunction(
                new TimeFormatter.RelativeTimeFormatter(false, false, false, true, true),
                new LogFileHandler(logDir, new DateTimeFileCreationStrategy(
                        DateTimeFileCreationStrategy.DEFAULT_DATE_FILE_NAME_FORMATTER,
                        false,
                        DateTimeFileCreationStrategy.DEFAULT_LOG_FILE_EXTENSION,
                        512 * 1000,
                        Duration.ofDays(60)), false,
                        freeSpace - bytesToLog)));

        final String logMessage = "This is just some message that is somewhat long. There are longer, but we can just loop more.";
        final int loopTimes = (int) (bytesToLog / logMessage.length() * 2);
        System.out.println("Will loop "+loopTimes+" times");

        final Logger LOG = LoggerFactory.getLogger("remainingSpaceTest");
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

}
