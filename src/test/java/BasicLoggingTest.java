import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.FileLogFunction;
import com.darkyen.tproll.logfunctions.LogFileCreationStrategy;
import com.darkyen.tproll.logfunctions.LogFileHandler;
import com.darkyen.tproll.util.TimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 *
 */
public class BasicLoggingTest {

    private static void logThings(Logger LOG) {
        for (int i = 1; i < TPLogger.LOG; i++) {
            switch (i) {
                case TPLogger.TRACE:
                    TPLogger.TRACE();
                    break;
                case TPLogger.DEBUG:
                    TPLogger.DEBUG();
                    break;
                case TPLogger.INFO:
                    TPLogger.INFO();
                    break;
                case TPLogger.WARN:
                    TPLogger.WARN();
                    break;
                case TPLogger.ERROR:
                    TPLogger.ERROR();
                    break;
            }
            LOG.trace("Out of the mainframe");
            LOG.debug("I'm coming {}", "undone");
            LOG.info("You can't catch a digital shadow");
            LOG.warn("Deep into the city grid I go");
            LOG.error("Freedom is a ", "green light");
        }
    }


    public static void main(String[] args) throws Exception {
        TPLogger.attachUnhandledExceptionLogger();
        final Logger LOG = LoggerFactory.getLogger("TestLogger");
        logThings(LOG);

        TPLogger.setLogFunction(new FileLogFunction(new TimeFormatter.AbsoluteTimeFormatter(), new LogFileHandler(new File("test logs"), LogFileCreationStrategy.createDefaultDateStrategy(), true), true));
        logThings(LOG);

        throw new Exception("Test exception");
    }
}
