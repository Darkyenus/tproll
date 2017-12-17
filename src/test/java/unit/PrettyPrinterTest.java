package unit;

import com.darkyen.tproll.util.PrettyPrinter;
import org.junit.Test;

import java.io.File;
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

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Additional pretty-printer tests
 */
public class PrettyPrinterTest {

    @Test
    public void simpleAbsoluteFile() {
        assertEquals(new File("").getAbsolutePath()+"/", PrettyPrinter.toString(new File(".").getAbsoluteFile()));
        assertEquals(new File("wo").getAbsolutePath()+" ⌫", PrettyPrinter.toString(new File("./whatever/../wo").getAbsoluteFile()));
    }

    @Test
    public void simpleRelativeFile() {
        assertEquals("./", PrettyPrinter.toString(new File(".")));
        assertEquals("wo ⌫", PrettyPrinter.toString(new File("./whatever/../wo")));
    }

    @Test
    public void rootedFiles() {
        try {
            PrettyPrinter.setApplicationRootDirectory(new File("./1234"));
            assertEquals(new File("").getAbsolutePath() + "/", PrettyPrinter.toString(new File(".").getAbsoluteFile()));
            assertEquals(new File("wo").getAbsolutePath() + " ⌫", PrettyPrinter.toString(new File("./whatever/../wo").getAbsoluteFile()));

            assertEquals(". ⌫", PrettyPrinter.toString(new File("./1234/").getAbsoluteFile()));
            assertEquals("wo ⌫", PrettyPrinter.toString(new File("./1234/whatever/../wo").getAbsoluteFile()));
        } finally {
            PrettyPrinter.setApplicationRootDirectory((Path) null);
        }
    }

    @Test
    public void symlinks() throws Exception {
        // .toRealPath() is needed, because sometimes tempRoot itself contains symlinks
        final Path tempRoot = Files.createTempDirectory("PrettyPrintTest").toRealPath();

        final Path simple = tempRoot.resolve("simple");
        Files.createFile(simple);

        final Path valid_link = tempRoot.resolve("valid_link");
        Files.createSymbolicLink(valid_link, tempRoot.relativize(simple));

        final Path non_existent = tempRoot.resolve("non_existent");

        final Path invalid_link = tempRoot.resolve("invalid_link");
        Files.createSymbolicLink(invalid_link, tempRoot.relativize(non_existent));

        final Path directory = tempRoot.resolve("directory");
        Files.createDirectory(directory);

        final Path directory_link = tempRoot.resolve("directory_link");
        Files.createSymbolicLink(directory_link, directory);

        try {
            PrettyPrinter.setApplicationRootDirectory(tempRoot);

            assertEquals("simple", PrettyPrinter.toString(simple));
            assertEquals("valid_link → "+simple, PrettyPrinter.toString(valid_link));
            assertEquals("non_existent ⌫", PrettyPrinter.toString(non_existent));
            assertEquals("invalid_link ⇥", PrettyPrinter.toString(invalid_link));
            assertEquals("directory/", PrettyPrinter.toString(directory));
            assertEquals("directory_link/ → "+directory, PrettyPrinter.toString(directory_link));
        } finally {
            PrettyPrinter.setApplicationRootDirectory((Path) null);
        }
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
            assertEquals("HashMap{1 entry}", PrettyPrinter.toString(hashMap, 0));
            assertEquals("HashMap{uno=simple}", PrettyPrinter.toString(hashMap, 1));
            hashMap.put(simple, "dos");
            assertThat(PrettyPrinter.toString(hashMap), anyOf(
                    is("HashMap{uno=simple, simple=dos}"),
                    is("HashMap{simple=dos, uno=simple}")));
            assertEquals("HashMap{2 entries}", PrettyPrinter.toString(hashMap, 0));
            assertThat(PrettyPrinter.toString(hashMap, 1), anyOf(
                    is("HashMap{uno=simple, ... (1 more)}"),
                    is("HashMap{simple=dos, ... (1 more)}")
            ));

            final Map<Object, Object> phonyMap = new PhonyMap<>();
            phonyMap.put("uno", simple);
            assertEquals("PhonyMap{1 entry}", PrettyPrinter.toString(phonyMap, 0));
            assertEquals("PhonyMap{uno=simple}", PrettyPrinter.toString(phonyMap, 1));
            phonyMap.put(simple, "dos");
            assertEquals("PhonyMap{uno=simple, simple=dos}", PrettyPrinter.toString(phonyMap));
            assertEquals("PhonyMap{2 entries}", PrettyPrinter.toString(phonyMap, 0));
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

    private static final class PhonyList<T> extends ArrayList<T> {}
    private static final class PhonyMap<K, V> extends LinkedHashMap<K, V> {}
}
