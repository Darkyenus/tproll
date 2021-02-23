package com.darkyen.tproll.util;

import org.jetbrains.annotations.NotNull;

/**
 * ANSI terminal colors helper constants.
 */
public final class AnsiColor {

    /** Whether the current OS is likely to support ANSI terminal colors */
    @SuppressWarnings("WeakerAccess")
    public static final boolean COLOR_SUPPORTED;

    static {
        //Check property tproll.color and then environment variable "TPROLL_COLOR"
        final String env = System.getProperty("tproll.color", System.getenv("TPROLL_COLOR"));

        if (env != null) {
            COLOR_SUPPORTED = "true".equalsIgnoreCase(env);
        } else {
            // No environment variable, do heuristic
            // If we are on windows or on android, disable, otherwise enable
            final String os = System.getProperty("os.name");
            final String vendor = System.getProperty("java.vendor");
            //noinspection RedundantIfStatement
            if (os == null || os.toLowerCase().contains("win")
                    || (vendor != null && vendor.toLowerCase().contains("android"))) {
                COLOR_SUPPORTED = false;
            } else {
                COLOR_SUPPORTED = true;
            }
        }
    }

    public static final @NotNull String RESET = "\u001B[0m";
    public static final @NotNull String BLACK = "\u001B[30m";
    public static final @NotNull String RED = "\u001B[31m";
    public static final @NotNull String GREEN = "\u001B[32m";
    public static final @NotNull String YELLOW = "\u001B[33m";
    public static final @NotNull String BLUE = "\u001B[34m";
    public static final @NotNull String PURPLE = "\u001B[35m";
    public static final @NotNull String CYAN = "\u001B[36m";
    public static final @NotNull String WHITE = "\u001B[37m";

    private AnsiColor() {/* utility class */}
}
