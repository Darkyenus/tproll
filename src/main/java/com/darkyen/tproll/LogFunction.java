package com.darkyen.tproll;

import com.darkyen.tproll.util.TimeFormatter;

import java.io.PrintStream;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import static com.darkyen.tproll.util.TerminalColor.*;

public interface LogFunction {
    /**
     * Called when logger needs to log a message. Called only when that log level is enabled in the logger.
     * Can be called by any thread, and thus MUST be thread safe.
     *
     * @param name    of the logger
     * @param time    in ms since start of the app
     * @param level   of this message
     * @param content of this message, formatted. Do not keep around!
     * @param error   holding the stack trace logging function should handle
     */
    void log(String name, long time, byte level, CharSequence content, Throwable error);

    LogFunction SIMPLE_LOG_FUNCTION = new LogFunction() {

        private final StringBuilder sb = new StringBuilder();
        private final TimeFormatter absoluteTimeFormatter = new TimeFormatter.AbsoluteTimeFormatter(new DateTimeFormatterBuilder()
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE)
                .toFormatter());
        private final TimeFormatter relativeTimeFormatter = new TimeFormatter.RelativeTimeFormatter(false, true, true, true, false);

        @Override
        public synchronized void log(String name, long time, byte level, CharSequence content, Throwable error) {
            PrintStream out = level <= TPLogger.INFO || level == TPLogger.LOG ? System.out : System.err;
            final StringBuilder sb = this.sb;
            black(sb);
            sb.append('[');
            blue(sb);
            if (time < (1000L * 60 * 60 * 24 * 365 * 10)) {//Less than 10 years? (lets assume that no system with this logger will have 10 year uptime)
                relativeTimeFormatter.format(time, sb);
            } else {
                absoluteTimeFormatter.format(time, sb);
            }
            cyan(sb);
            sb.append(' ').append(name);
            black(sb);
            sb.append(']');
            purple(sb);
            sb.append(' ');
            sb.append(TPLogger.levelName(level));
            black(sb);
            sb.append(':');
            sb.append(' ');
            reset(sb);

            sb.append(content);

            out.append(sb);

            sb.setLength(0);

            if (error != null) {
                error.printStackTrace(out);
            }
        }
    };
}
