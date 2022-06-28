package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.util.TimeFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Marker;

import java.io.File;
import java.time.Duration;

import static com.darkyen.tproll.util.RenderableMarker.appendMarker;

/**
 * LogFunction which logs to a file.
 * Actual file handling is done through {@link ILogFileHandler} interface.
 *
 * @see FileLogFunction default implementation of ILogFileHandler
 */
public class FileLogFunction extends LogFunction {

    private final @NotNull Object LOCK = new Object();
    private final @Nullable TimeFormatter timeFormatter;
    private final @NotNull ILogFileHandler logFileHandler;

    private boolean logging = false;

    /**
     * @param timeFormatter used for displaying time, null for no time
     * @param logFileHandler for file handling
     */
    public FileLogFunction(@Nullable TimeFormatter timeFormatter, @NotNull ILogFileHandler logFileHandler) {
        this.timeFormatter = timeFormatter;
        this.logFileHandler = logFileHandler;
    }

        /**
         * Shortcut constructor for most common usage.
         * Messages are logged with absolute time, to files with date in name and default extension (.log).
         * These files are not appended to, as they are compressed on exit.
         * When logs take over 500MB, oldest ones are deleted, but not those younger than 60 days.
         * The logging will stop when there is less than 500MB remaining on disk.
         *
         * @param logDirectory to place logs in
         */
    public FileLogFunction(@NotNull File logDirectory) {
        this(
                new TimeFormatter.AbsoluteTimeFormatter(),
                new LogFileHandler(
                        logDirectory,
                        new DateTimeFileCreationStrategy(
                                DateTimeFileCreationStrategy.DEFAULT_DATE_FILE_NAME_FORMATTER,
                                false,
                                DateTimeFileCreationStrategy.DEFAULT_LOG_FILE_EXTENSION,
                                500 * 1000,
                                Duration.ofDays(60)),
                        true, 500_000_000/*500MB*/, 500_000_000/*500MB*/, true));
    }

    private final @NotNull StringBuilder log_sb = new StringBuilder();

    @Override
    public boolean log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
        synchronized (LOCK) {
            if (logging) {
                return false;
            }
            final StringBuilder sb = this.log_sb;
            try {
                logging = true;// Do not log to file when something inside this logs

                sb.append('[');
                if (timeFormatter != null) {
                    timeFormatter.format(time, sb);
                    sb.append(' ');
                }
                sb.append(alignedLevelName(level));
                if (marker != null) {
                    appendMarker(sb, false, marker, true);
                }
                sb.append(']').append(' ').append(name).append(':').append(' ');
                sb.append(content).append('\n');

                return logFileHandler.log(sb);
            } finally {
                logging = false;
                sb.setLength(0);
            }
        }
    }

    @Override
    public synchronized void start() {
        try {
            logFileHandler.start();
        } finally {
            super.start();
        }
    }

    @Override
    public synchronized void stop() {
        try {
            logFileHandler.stop();
        } finally {
            super.stop();
        }
    }

    public static @NotNull String alignedLevelName(byte logLevel){
        switch (logLevel) {
            case TPLogger.TRACE: return "TRACE";
            case TPLogger.DEBUG: return "DEBUG";
            case TPLogger.INFO:  return "INFO ";
            case TPLogger.WARN:  return "WARN ";
            case TPLogger.ERROR: return "ERROR";
            case TPLogger.LOG:   return "LOG  ";
            default: return "UNKNOWN LEVEL "+logLevel;
        }
    }
}
