package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.util.AnsiColor;
import com.darkyen.tproll.util.TimeFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Marker;

import java.io.PrintStream;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import static com.darkyen.tproll.util.RenderableMarker.appendMarker;

/**
 * Simple log function, basis for other custom implementations.
 * Logging of time is configurable.
 *
 * @see SimpleLogFunction#CONSOLE_LOG_FUNCTION for implementation that logs into the console
 */
public abstract class SimpleLogFunction extends LogFunction {

    private final @NotNull StringBuilder sb = new StringBuilder();
    private final @Nullable TimeFormatter absoluteTimeFormatter;
    private final @Nullable TimeFormatter relativeTimeFormatter;
    protected final boolean ansiColor;

    @SuppressWarnings("unused")
    public SimpleLogFunction(@Nullable TimeFormatter absoluteTimeFormatter, @Nullable TimeFormatter relativeTimeFormatter, boolean ansiColor) {
        this.absoluteTimeFormatter = absoluteTimeFormatter;
        this.relativeTimeFormatter = relativeTimeFormatter;
        this.ansiColor = AnsiColor.COLOR_SUPPORTED;
    }

    @SuppressWarnings("unused")
    public SimpleLogFunction(@Nullable TimeFormatter absoluteTimeFormatter, @Nullable TimeFormatter relativeTimeFormatter) {
        this(absoluteTimeFormatter, relativeTimeFormatter, AnsiColor.COLOR_SUPPORTED);
    }

    public SimpleLogFunction(boolean ansiColor) {
        this(new TimeFormatter.AbsoluteTimeFormatter(new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter()),
                new TimeFormatter.RelativeTimeFormatter(false, true, true, true, false),
                ansiColor);
    }

    public SimpleLogFunction() {
        this(AnsiColor.COLOR_SUPPORTED);
    }

    @Override
    public final synchronized boolean log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
        final boolean color = this.ansiColor;
        final StringBuilder sb = this.sb;
        try {
            if (color) sb.append(AnsiColor.BLACK);
            sb.append('[');
            if (color) sb.append(AnsiColor.BLUE);
            if (relativeTimeFormatter != null && (time < (1000L * 60 * 60 * 24 * 365 * 20) || absoluteTimeFormatter == null)) {
                // Less than 20 years? (lets assume that no system with this logger will have more years of uptime)
                relativeTimeFormatter.format(time, sb);
                sb.append(' ');
            } else if (absoluteTimeFormatter != null) {
                absoluteTimeFormatter.format(time, sb);
                sb.append(' ');
            }

            switch (level) {
                case TPLogger.TRACE: {
                    if (color) sb.append(AnsiColor.WHITE);
                    sb.append("TRACE");
                    break;
                }
                case TPLogger.DEBUG: {
                    if (color) sb.append(AnsiColor.GREEN);
                    sb.append("DEBUG");
                    break;
                }
                case TPLogger.INFO: {
                    if (color) sb.append(AnsiColor.CYAN);
                    sb.append("INFO ");
                    break;
                }
                case TPLogger.WARN: {
                    if (color) sb.append(AnsiColor.YELLOW);
                    sb.append("WARN ");
                    break;
                }
                case TPLogger.ERROR: {
                    if (color) sb.append(AnsiColor.RED);
                    sb.append("ERROR");
                    break;
                }
                case TPLogger.LOG: {
                    if (color) sb.append(AnsiColor.BLUE);
                    sb.append("LOG  ");
                    break;
                }
                default: {
                    if (color) sb.append(AnsiColor.RED);
                    sb.append("UNKNOWN LEVEL ").append(level);
                    break;
                }
            }

            if (color) sb.append(AnsiColor.BLACK);
            if (marker != null) {
                appendMarker(sb, color, marker, true);
            }
            if (color) sb.append(AnsiColor.BLACK);
            sb.append(']');
            if (color) sb.append(AnsiColor.PURPLE);
            sb.append(' ');
            sb.append(name);
            if (color) sb.append(AnsiColor.BLACK);
            sb.append(':');
            sb.append(' ');
            if (color) sb.append(AnsiColor.RESET);

            sb.append(content);

            logLine(level, sb);
            return true;
        } finally {
            sb.setLength(0);
        }
    }

    protected abstract void logLine(byte level, @NotNull CharSequence formattedContent);

    /** Implementation of {@link SimpleLogFunction} which logs to stdout and stderr. */
    public static final SimpleLogFunction CONSOLE_LOG_FUNCTION = new SimpleLogFunction() {

        private @Nullable PrintStream log_lastStream;

        @Override
        protected void logLine(byte level, @NotNull CharSequence formattedContent) {
            PrintStream out = (level <= TPLogger.INFO || level == TPLogger.LOG || AnsiColor.COLOR_SUPPORTED) ? System.out : System.err;
            if (log_lastStream != out) {
                if (log_lastStream != null){
                    log_lastStream.flush();//To preserve out/err order
                }
                log_lastStream = out;
            }

            out.println(formattedContent);
        }

        @Override
        public void stop() {
            final PrintStream lastStream = this.log_lastStream;
            if (lastStream != null) {
                lastStream.flush();
            }
        }
    };

    public static final SimpleLogFunction EMERGENCY_LOG_FUNCTION = new SimpleLogFunction(false) {
        @Override
        protected void logLine(byte level, @NotNull CharSequence formattedContent) {
            PrintStream out = System.err;
            try {
                out.println(formattedContent);
                out.flush();
            } catch (Throwable ignored) {
                // What can we do? Nothing.
            }
        }
    };
}
