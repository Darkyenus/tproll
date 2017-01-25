package com.darkyen.tproll.util;

/**
 *
 */
public class TerminalColor {

    @SuppressWarnings("WeakerAccess")
    public static final boolean COLOR_SUPPORTED;

    static {
        //Check environment variable "TPROLL_COLOR"
        final String env = System.getenv("TPROLL_COLOR");
        if (env != null) {
            COLOR_SUPPORTED = "true".equalsIgnoreCase(env) || "yes".equalsIgnoreCase(env) || "1".equalsIgnoreCase(env);
        } else {
            // No environment variable, do heuristic
            // If we are on windows, disable, otherwise enable
            final String os = System.getProperty("os.name");
            //noinspection RedundantIfStatement
            if (os == null || os.toLowerCase().contains("win")) {
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
