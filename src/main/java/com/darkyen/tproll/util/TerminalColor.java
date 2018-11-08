package com.darkyen.tproll.util;

/**
 * Simple implementation of ANSI terminal colors.
 */
public class TerminalColor {

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

    public static void reset(StringBuilder sb) {
        if (COLOR_SUPPORTED) {
            sb.append("\u001B[0m");
        }
    }

    public static void black(StringBuilder sb) {
        if (COLOR_SUPPORTED) {
            sb.append("\u001B[30m");
        }
    }

    public static void red(StringBuilder sb) {
        if (COLOR_SUPPORTED) {
            sb.append("\u001B[31m");
        }
    }

    public static void green(StringBuilder sb) {
        if (COLOR_SUPPORTED) {
            sb.append("\u001B[32m");
        }
    }

    public static void yellow(StringBuilder sb) {
        if (COLOR_SUPPORTED) {
            sb.append("\u001B[33m");
        }
    }

    public static void blue(StringBuilder sb) {
        if (COLOR_SUPPORTED) {
            sb.append("\u001B[34m");
        }
    }

    public static void purple(StringBuilder sb) {
        if (COLOR_SUPPORTED) {
            sb.append("\u001B[35m");
        }
    }

    public static void cyan(StringBuilder sb) {
        if (COLOR_SUPPORTED) {
            sb.append("\u001B[36m");
        }
    }

    public static void white(StringBuilder sb) {
        if (COLOR_SUPPORTED) {
            sb.append("\u001B[37m");
        }
    }

}
