package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.util.TerminalColor;
import com.darkyen.tproll.util.TimeFormatter;
import org.slf4j.Marker;

import java.io.PrintStream;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import static com.darkyen.tproll.util.TerminalColor.*;

/**
 * Simple log function, that logs to the stdout and stderr.
 * Logging of time is configurable.
 */
public final class ConsoleLogFunction implements LogFunction {

    private final StringBuilder sb = new StringBuilder();
    private final TimeFormatter absoluteTimeFormatter;
    private final TimeFormatter relativeTimeFormatter;

    private PrintStream log_lastStream;

    public ConsoleLogFunction() {
        absoluteTimeFormatter = new TimeFormatter.AbsoluteTimeFormatter(new DateTimeFormatterBuilder()
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .toFormatter());
        relativeTimeFormatter = new TimeFormatter.RelativeTimeFormatter(false, true, true, true, false);
    }

    public ConsoleLogFunction(TimeFormatter absoluteTimeFormatter, TimeFormatter relativeTimeFormatter) {
        this.absoluteTimeFormatter = absoluteTimeFormatter;
        this.relativeTimeFormatter = relativeTimeFormatter;
    }

    @Override
    public synchronized void log(String name, long time, byte level, Marker marker, CharSequence content) {
        PrintStream out = (level <= TPLogger.INFO || level == TPLogger.LOG || TerminalColor.COLOR_SUPPORTED) ? System.out : System.err;
        if (log_lastStream != out) {
            if (log_lastStream != null){
                log_lastStream.flush();//To preserve out/err order
            }
            log_lastStream = out;
        }

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

        out.append(sb).append('\n');

        sb.setLength(0);
    }
}
