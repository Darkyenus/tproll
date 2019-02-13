package com.darkyen.tproll.util.prettyprint;

import com.darkyen.tproll.util.PrettyPrinter;

import java.io.File;

/**
 * Implementation of pretty printing of {@link File}s.
 */
public class PrettyPrinterFileModule implements PrettyPrinter.PrettyPrinterModule {
	@Override
	public boolean append(StringBuilder sb, Object item, int maxCollectionElements) {
		if (!(item instanceof File)) {
			return false;
		}
		final File file = (File)item;

		final File absoluteFile = file.getAbsoluteFile();
		File canonicalFile = null;
		try {
			canonicalFile = file.getCanonicalFile();
		} catch (Exception ignored) {}

		if (canonicalFile == null) {
			sb.append(absoluteFile.getPath());
		} else {
			sb.append(canonicalFile.getPath());
		}

		if (absoluteFile.isDirectory()) {
			sb.append('/');
		} else if (!absoluteFile.exists()) {
			sb.append(" ⌫");
		}

		return true;
	}
}
