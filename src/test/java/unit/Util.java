package unit;

import org.jetbrains.annotations.NotNull;

public class Util {

	/** Strip basic ansi color tags, not production quality. */
	public static @NotNull String stripAnsi(@NotNull String from) {
		return from.replaceAll("\u001B\\[[0-9;]+m", "");
	}

}
