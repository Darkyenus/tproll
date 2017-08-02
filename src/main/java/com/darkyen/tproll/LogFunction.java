package com.darkyen.tproll;

import com.darkyen.tproll.util.TerminalColor;
import com.darkyen.tproll.util.TimeFormatter;
import org.slf4j.Marker;

import java.io.PrintStream;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import static com.darkyen.tproll.util.TerminalColor.*;

public interface LogFunction {
    /**
     * Called when logger needs to log a message. Called only when that log level is enabled in the logger.
     * Can be called by any thread, and thus MUST be thread safe.
     * @param name of the logger
     * @param time in ms since start of the app or since 1970
     * @param level of this message
     * @param marker provided or null
     * @param content of this message, formatted, without trailing newline. Do not keep around!
     */
    void log(String name, long time, byte level, Marker marker, CharSequence content);

    /**
     * Additional check whether this log function will log message of given level/marker.
     * This is only secondary check, primary level check is done through log level of TPLogger.
     * @return if such message would be logged
     */
    default boolean isEnabled(byte level, Marker marker){
        return true;
    }

    LogFunction SIMPLE_LOG_FUNCTION = new LogFunction() {

        private final StringBuilder sb = new StringBuilder();
        private final TimeFormatter absoluteTimeFormatter = new TimeFormatter.AbsoluteTimeFormatter(new DateTimeFormatterBuilder()
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .toFormatter());
        private final TimeFormatter relativeTimeFormatter = new TimeFormatter.RelativeTimeFormatter(false, true, true, true, false);

        private PrintStream log_lastStream;

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
            if (time < (1000L * 60 * 60 * 24 * 365 * 20)) {//Less than 20 years? (lets assume that no system with this logger will have more years of uptime)
                relativeTimeFormatter.format(time, sb);
            } else {
                absoluteTimeFormatter.format(time, sb);
            }
            sb.append(' ');

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
    };
}
