package unit;

import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.TPLoggerFactory;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.PrintStream;
import java.io.PrintWriter;

import static org.junit.Assert.assertThat;

/**
 * Test for TPLogger
 */
@SuppressWarnings("serial")
public class LoggerTest {

    private static final String LOGGER_NAME = "TEST-LOGGER";
    private static TPLogger log;
    private static StringBuilder logOut;

    private static final Object LOGGER_LOCK = new Object();

    @BeforeClass
    public static void setUp() {
        log = new TPLoggerFactory().getLogger(LOGGER_NAME);
        logOut = new StringBuilder();
        TPLogger.setLogFunction((name, time, level, marker, content) -> {
            synchronized (LOGGER_LOCK) {
                logOut.append(name).append(" ").append(time).append(" [").append(TPLogger.levelName(level)).append("]: ").append(content);
            }
        });
    }

    @AfterClass
    public static void tearDown() {
        log = null;
        logOut = null;
    }

    @Before
    public void prepare() {
        TPLogger.TRACE();
        logOut.setLength(0);
    }

    private void assertLogIs(String logContent) {
        assertThat(logOut.toString(), new CustomTypeSafeMatcher<String>("matching regex \"" + logContent + "\"") {
            @Override
            protected boolean matchesSafely(String item) {
                return (item).matches(logContent);
            }
        });
    }

    private static final String INFO_PREFIX = LOGGER_NAME + " \\d+ \\[INFO\\]: ";

    @Test
    public void noArgTest() {
        log.info("Sample message");
        assertLogIs(INFO_PREFIX + "Sample message");
    }

    @Test
    public void simpleArgTest() {
        log.info("Answer is {}", 42);
        assertLogIs(INFO_PREFIX + "Answer is 42");
    }

    @Test
    public void doubleArgTest() {
        log.info("Fav colors: {}, {}", "blue", "black");
        assertLogIs(INFO_PREFIX + "Fav colors: blue, black");
    }

    @Test
    public void manyArgTest() {
        log.info("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        assertLogIs(INFO_PREFIX + "1 2 3 4 5 6 7");
    }

    @Test
    public void tooManyArgTest() {
        log.info("I want {} and {}", "ice-cream", "biscuits", "coffee", "donuts");
        assertLogIs(INFO_PREFIX + "I want ice-cream and biscuits \\{coffee, donuts\\}");
    }

    @Test
    public void noSubstitutionException() {
        log.info("Problem probably", new DummyException(""));
        assertLogIs(INFO_PREFIX + "Problem probably\nstack-trace-here");
    }

    @Test
    public void substitutedException() {
        log.info("Problem probably {}", new DummyException("potato"));
        assertLogIs(INFO_PREFIX + "Problem probably java.lang.Exception: potato\nstack-trace-here");
    }

    @Test
    public void notLastException() {
        log.info("Problem probably {} with {}", new DummyException("tomato"), "ice-cream");
        assertLogIs(INFO_PREFIX + "Problem probably java.lang.Exception: tomato with ice-cream\nstack-trace-here");
    }

    @Test
    public void notLastExceptionAndMore() {
        log.info("{} is not food", "raisin", new DummyException("not food"));
        assertLogIs(INFO_PREFIX + "raisin is not food\nstack-trace-here");
    }

    @Test
    public void notLastExceptionAndEvenMore() {
        log.info("{} is not food", "raisin", new DummyException("not food"), "1234");
        assertLogIs(INFO_PREFIX + "raisin is not food \\{java.lang.Exception: not food, 1234\\}\nstack-trace-here");
    }

    @Test
    public void emptyArray() {
        log.info("I have these banknotes: {}", new int[0]);
        assertLogIs(INFO_PREFIX + "I have these banknotes: int\\[\\]");
    }

    @Test
    public void fullArray() {
        log.info("I have these banknotes: {}", (Object)new String[]{"100","200","-560","1M Zimbabwe $"});
        assertLogIs(INFO_PREFIX + "I have these banknotes: String\\[100, 200, -560, 1M Zimbabwe \\$\\]");
    }

    @Test
    public void nullArg() {
        log.info("Reason: {}", (Object)null);
        assertLogIs(INFO_PREFIX + "Reason: null");
    }

    @SuppressWarnings("serial")
    private static final class DummyException extends Exception {
        public DummyException(String message) {
            super(message);
        }

        @Override
        public void printStackTrace(PrintStream s) {
            s.append("stack-trace-here");
        }

        @Override
        public void printStackTrace(PrintWriter s) {
            s.append("stack-trace-here");
        }

        @Override
        public String toString() {
            return "java.lang.Exception: "+getMessage();
        }
    }
}
