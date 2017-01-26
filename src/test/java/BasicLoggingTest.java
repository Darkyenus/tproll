import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.FileLogFunction;
import com.darkyen.tproll.logfunctions.LogFileCreationStrategy;
import com.darkyen.tproll.logfunctions.LogFileHandler;
import com.darkyen.tproll.logfunctions.LogFunctionMultiplexer;
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
        TPLogger.setLogFunction(
                new LogFunctionMultiplexer(
                        LogFunction.SIMPLE_LOG_FUNCTION, // Log to console
                        new FileLogFunction(new File("test logs")) // & Log to file in "test logs" directory
                ));


        final Logger LOG = LoggerFactory.getLogger(BasicLoggingTest.class);
        LOG.info("I log {}", "string parameter");

        TPLogger.attachUnhandledExceptionLogger();

        LOG.warn("tproll {} !", (System.currentTimeMillis() & 1) == 0 ? "is great" : "rules");

        logThings(LOG);

        throw new Exception("Test exception");
    }
}
