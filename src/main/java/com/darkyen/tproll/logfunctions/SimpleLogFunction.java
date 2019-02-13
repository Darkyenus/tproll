package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.util.TerminalColor;
import com.darkyen.tproll.util.TimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.slf4j.Marker;

import java.io.PrintStream;

import static com.darkyen.tproll.util.TerminalColor.*;

/**
 * Simple log function, basis for other custom implementations.
 * Logging of time is configurable.
 *
 * @see SimpleLogFunction#CONSOLE_LOG_FUNCTION for implementation that logs into the console
 */
public abstract class SimpleLogFunction extends LogFunction {

    private final StringBuilder sb = new StringBuilder();
    private final TimeFormatter absoluteTimeFormatter;
    private final TimeFormatter relativeTimeFormatter;

    public SimpleLogFunction() {
        absoluteTimeFormatter = new TimeFormatter.AbsoluteTimeFormatter(new DateTimeFormatterBuilder()
                .appendHourOfDay(2)
                .appendLiteral(':')
                .appendMinuteOfHour(2)
                .appendLiteral(':')
                .appendSecondOfMinute(2)
                .toFormatter());
        relativeTimeFormatter = new TimeFormatter.RelativeTimeFormatter(false, true, true, true, false);
    }

    @SuppressWarnings("unused")
    public SimpleLogFunction(TimeFormatter absoluteTimeFormatter, TimeFormatter relativeTimeFormatter) {
        this.absoluteTimeFormatter = absoluteTimeFormatter;
        this.relativeTimeFormatter = relativeTimeFormatter;
    }

    @Override
    public final synchronized void log(String name, long time, byte level, Marker marker, CharSequence content) {
        final StringBuilder sb = this.sb;
        black(sb);
        sb.append('[');
        blue(sb);
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
                white(sb);
                sb.append("TRACE");
                break;
            }
            case TPLogger.DEBUG: {
                green(sb);
                sb.append("DEBUG");
                break;
            }
            case TPLogger.INFO: {
                cyan(sb);
                sb.append("INFO ");
                break;
            }
            case TPLogger.WARN: {
                yellow(sb);
                sb.append("WARN ");
                break;
            }
            case TPLogger.ERROR: {
                red(sb);
                sb.append("ERROR");
                break;
            }
            case TPLogger.LOG: {
                blue(sb);
                sb.append("LOG  ");
                break;
            }
            default:  {
                red(sb);
                sb.append("UNKNOWN LEVEL ").append(level);
                break;
            }
        }

        black(sb);
        sb.append(']');
        purple(sb);
        sb.append(' ');
        sb.append(name);
        black(sb);
        sb.append(':');
        sb.append(' ');
        reset(sb);

        sb.append(content);

        logLine(level, sb);

        sb.setLength(0);
    }

    protected abstract void logLine(byte level, CharSequence formattedContent);

    /** Implementation of {@link SimpleLogFunction} which logs to stdout and stderr. */
    public static final SimpleLogFunction CONSOLE_LOG_FUNCTION = new SimpleLogFunction() {

        private PrintStream log_lastStream;

        @Override
        protected void logLine(byte level, CharSequence formattedContent) {
            PrintStream out = (level <= TPLogger.INFO || level == TPLogger.LOG || TerminalColor.COLOR_SUPPORTED) ? System.out : System.err;
            if (log_lastStream != out) {
                if (log_lastStream != null){
                    log_lastStream.flush();//To preserve out/err order
                }
                log_lastStream = out;
            }

            out.println(formattedContent);
        }
    };
}
