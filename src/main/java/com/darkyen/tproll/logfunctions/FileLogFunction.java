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

    private boolean logFileHandlerInitialized = false;

    /**
     * @param timeFormatter used for displaying time, null for no time
     * @param logFileHandler for file handling
     * @param registerShutdownHook to automatically call dispose (and flush log files!) when the application shuts down. Recommended: true.
     */
    public FileLogFunction(@Nullable TimeFormatter timeFormatter, @NotNull ILogFileHandler logFileHandler, boolean registerShutdownHook) {
        this.timeFormatter = timeFormatter;
        this.logFileHandler = logFileHandler;

        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::dispose));
        }
    }

    /**
     * Shortcut constructor for most common usage.
     * Messages are logged with absolute time, to files with date in name and default extension (.log).
     * These files are not appended to, as they are compressed on exit.
     * When logs take over 512MB, oldest ones are deleted, but not those younger than 60 days.
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
                                512 * 1000,
                                Duration.ofDays(60)),
                        true),
                true);
    }

    private final @NotNull StringBuilder log_sb = new StringBuilder();

    @Override
    public void log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
        synchronized (LOCK) {
            if (!logFileHandlerInitialized) {
                logFileHandlerInitialized = true;
                logFileHandler.initialize();
            }

            final StringBuilder sb = this.log_sb;
            sb.append('[');
            if (timeFormatter != null) {
                timeFormatter.format(time, sb);
                sb.append(' ');
            }
            sb.append(alignedLevelName(level));
            if (marker != null) {
                appendMarker(sb, marker, true, false);
            }
            sb.append(']').append(' ').append(name).append(':').append(' ');
            sb.append(content).append('\n');

            logFileHandler.log(sb);

            sb.setLength(0);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void dispose(){
        synchronized (LOCK) {
            if (logFileHandlerInitialized) {
                logFileHandler.dispose();
                logFileHandlerInitialized = false;
            }
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
