package com.darkyen.tproll.util.prettyprint;

import com.darkyen.tproll.util.PrettyPrinter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of pretty printing of {@link Path}s.
 *
 * These are available only on Java 8 and newer, so this class is loaded at runtime, and only when the JVM supports it.
 */
@SuppressWarnings({"unused"})
public class PrettyPrinterPathModule implements PrettyPrinter.PrettyPrinterModule {

	static {
		Path thisLineWillFailAndPreventLoadingOfThisClassOnOldJVM = Paths.get("");
	}

	private static @Nullable Path APPLICATION_ROOT_DIRECTORY = null;

	/** Applications with well specified root directory can put it here. All file paths under this directory will
	 * be printed out by this class in relative form. */
	public static @Nullable Path getApplicationRootDirectory() {
		return APPLICATION_ROOT_DIRECTORY;
	}

	/** @see #getApplicationRootDirectory() */
	public static void setApplicationRootDirectory(@Nullable Path applicationRootDirectory) {
		if (applicationRootDirectory == null) {
			APPLICATION_ROOT_DIRECTORY = null;
			return;
		}
		APPLICATION_ROOT_DIRECTORY = applicationRootDirectory.normalize().toAbsolutePath();
	}

	/** @see #getApplicationRootDirectory() */
	public static void setApplicationRootDirectory(@Nullable File applicationRootDirectory) {
		if (applicationRootDirectory == null) {
			APPLICATION_ROOT_DIRECTORY = null;
			return;
		}
		APPLICATION_ROOT_DIRECTORY = applicationRootDirectory.toPath().normalize().toAbsolutePath();
	}

	private static void appendPath(@NotNull StringBuilder sb, @NotNull Path path) {
		path = path.normalize();

		final Path root = APPLICATION_ROOT_DIRECTORY;
		final Path shownPath;
		if (root != null && path.startsWith(root)) {
			shownPath = root.relativize(path);
		} else {
			shownPath = path;
		}

		String showPathString = shownPath.toString();
		if (showPathString.isEmpty()) {
			// For empty strings (that is, current directory) write .
			sb.append('.');
		} else {
			sb.append(showPathString);
		}

		// Not following links, if this returns true, the file is simply not there
		if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			// File exists!
			if (Files.isDirectory(path)) {
				// It is a directory, indicate that
				sb.append('/');
			}

			if (!Files.exists(path)) {
				// File does not exist when following links, therefore it is a broken link
				sb.append(" ⇥");
			} else {
				// Where does the file lead when following links?
				Path leadsToPath = null;
				try {
					final Path realPath = path.toRealPath();
					if (!path.toAbsolutePath().equals(realPath)) {
						leadsToPath = realPath;
					}
				} catch (Throwable ignored) {}

				if (leadsToPath != null) {
					sb.append(" → ").append(leadsToPath.toString());
				}
			}
		} else {
			// File does not exist
			sb.append(" ⌫");
		}
	}

	@Override
	public boolean accepts(@NotNull Object item) {
		return item instanceof Path;
	}

	@Override
	public void append(@NotNull StringBuilder sb, @NotNull Object item, int maxCollectionElements) {
		appendPath(sb, (Path) item);
	}
}
