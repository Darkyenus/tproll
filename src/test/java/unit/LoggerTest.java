package unit;

import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.TPLoggerFactory;
import com.darkyen.tproll.util.PrettyPrinter;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Test for TPLogger
 */
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

    private void assertLogIs(String logContent, String message) {
        assertThat(message, logOut.toString(), new CustomTypeSafeMatcher<String>("matching regex \"" + logContent + "\"") {
            @Override
            protected boolean matchesSafely(String item) {
                return (item).matches(logContent);
            }
        });
    }

    private void assertLogIs(String logContent) {
        assertLogIs(logContent, "");
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

    @Test
    public void prettyPrintCollection() throws Exception {
        // .toRealPath() is needed, because sometimes tempRoot itself contains symlinks
        final Path tempRoot = Files.createTempDirectory("PrettyPrintTest").toRealPath();

        final Path simple = tempRoot.resolve("simple");
        Files.createFile(simple);

        try {
            PrettyPrinter.setApplicationRootDirectory(tempRoot);

            final List<Path> arrayList = new ArrayList<>();
            arrayList.add(simple);
            assertEquals("ArrayList[1 element]", PrettyPrinter.toString(arrayList, 0));
            arrayList.add(simple);
            assertEquals("ArrayList[simple, simple]", PrettyPrinter.toString(arrayList));
            assertEquals("ArrayList[2 elements]", PrettyPrinter.toString(arrayList, 0));
            assertEquals("ArrayList[simple, ... (1 more)]", PrettyPrinter.toString(arrayList, 1));

            final List<Path> phonyList = new PhonyList<>();
            phonyList.add(simple);
            assertEquals("PhonyList[1 element]", PrettyPrinter.toString(phonyList, 0));
            phonyList.add(simple);
            assertEquals("PhonyList[simple, simple]", PrettyPrinter.toString(phonyList));
            assertEquals("PhonyList[2 elements]", PrettyPrinter.toString(phonyList, 0));
            assertEquals("PhonyList[simple, ... (1 more)]", PrettyPrinter.toString(phonyList, 1));

            final List<Object> customList = new List<Object>() {
                @Override
                public int size() {
                    return 0;
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public boolean contains(Object o) {
                    return false;
                }

                @Override
                public Iterator<Object> iterator() {
                    return null;
                }

                @Override
                public Object[] toArray() {
                    return new Object[0];
                }

                @Override
                public <T> T[] toArray(T[] a) {
                    return null;
                }

                @Override
                public boolean add(Object o) {
                    return false;
                }

                @Override
                public boolean remove(Object o) {
                    return false;
                }

                @Override
                public boolean containsAll(Collection<?> c) {
                    return false;
                }

                @Override
                public boolean addAll(Collection<?> c) {
                    return false;
                }

                @Override
                public boolean addAll(int index, Collection<?> c) {
                    return false;
                }

                @Override
                public boolean removeAll(Collection<?> c) {
                    return false;
                }

                @Override
                public boolean retainAll(Collection<?> c) {
                    return false;
                }

                @Override
                public void clear() {

                }

                @Override
                public Object get(int index) {
                    return null;
                }

                @Override
                public Object set(int index, Object element) {
                    return null;
                }

                @Override
                public void add(int index, Object element) {

                }

                @Override
                public Object remove(int index) {
                    return null;
                }

                @Override
                public int indexOf(Object o) {
                    return 0;
                }

                @Override
                public int lastIndexOf(Object o) {
                    return 0;
                }

                @Override
                public ListIterator<Object> listIterator() {
                    return null;
                }

                @Override
                public ListIterator<Object> listIterator(int index) {
                    return null;
                }

                @Override
                public List<Object> subList(int fromIndex, int toIndex) {
                    return null;
                }

                @Override
                public String toString() {
                    return "Yo-yo, I'm so custom.";
                }
            };
            assertEquals("Yo-yo, I'm so custom.", PrettyPrinter.toString(customList));
        } finally {
            PrettyPrinter.setApplicationRootDirectory((Path) null);
        }
    }

    @Test
    public void prettyPrintMap() throws Exception {
        // .toRealPath() is needed, because sometimes tempRoot itself contains symlinks
        final Path tempRoot = Files.createTempDirectory("PrettyPrintTest").toRealPath();

        final Path simple = tempRoot.resolve("simple");
        Files.createFile(simple);

        try {
            PrettyPrinter.setApplicationRootDirectory(tempRoot);

            final Map<Object, Object> hashMap = new HashMap<>();
            hashMap.put("uno", simple);
            assertEquals("HashMap{1 pair}", PrettyPrinter.toString(hashMap, 0));
            assertEquals("HashMap{uno=simple}", PrettyPrinter.toString(hashMap, 1));
            hashMap.put(simple, "dos");
            assertEquals("HashMap{uno=simple, simple=dos}", PrettyPrinter.toString(hashMap));
            assertEquals("HashMap{2 pairs}", PrettyPrinter.toString(hashMap, 0));
            assertEquals("HashMap{uno=simple, ... (1 more)}", PrettyPrinter.toString(hashMap, 1));

            final Map<Object, Object> phonyMap = new PhonyMap<>();
            phonyMap.put("uno", simple);
            assertEquals("PhonyMap{1 pair}", PrettyPrinter.toString(phonyMap, 0));
            assertEquals("PhonyMap{uno=simple}", PrettyPrinter.toString(phonyMap, 1));
            phonyMap.put(simple, "dos");
            assertEquals("PhonyMap{uno=simple, simple=dos}", PrettyPrinter.toString(phonyMap));
            assertEquals("PhonyMap{2 pairs}", PrettyPrinter.toString(phonyMap, 0));
            assertEquals("PhonyMap{uno=simple, ... (1 more)}", PrettyPrinter.toString(phonyMap, 1));

            final Map<Object, Object> customMap = new Map<Object, Object>() {
                @Override
                public int size() {
                    return 0;
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public boolean containsKey(Object key) {
                    return false;
                }

                @Override
                public boolean containsValue(Object value) {
                    return false;
                }

                @Override
                public Object get(Object key) {
                    return null;
                }

                @Override
                public Object put(Object key, Object value) {
                    return null;
                }

                @Override
                public Object remove(Object key) {
                    return null;
                }

                @Override
                public void putAll(Map<?, ?> m) {

                }

                @Override
                public void clear() {

                }

                @Override
                public Set<Object> keySet() {
                    return null;
                }

                @Override
                public Collection<Object> values() {
                    return null;
                }

                @Override
                public Set<Entry<Object, Object>> entrySet() {
                    return null;
                }

                @Override
                public String toString() {
                    return "Yo-yo, I'm so custom.";
                }
            };
            assertEquals("Yo-yo, I'm so custom.", PrettyPrinter.toString(customMap));
        } finally {
            PrettyPrinter.setApplicationRootDirectory((Path) null);
        }
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

    private static final class PhonyList<T> extends ArrayList<T> {}
    private static final class PhonyMap<K, V> extends LinkedHashMap<K, V> {}
}
