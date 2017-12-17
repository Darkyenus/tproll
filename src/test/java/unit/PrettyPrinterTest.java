package unit;

import com.darkyen.tproll.util.PrettyPrinter;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

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
}
