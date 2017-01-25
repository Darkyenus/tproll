package com.darkyen.tproll.advanced;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.util.TimeFormatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 *
 */
public class FileLogFunction implements LogFunction {

    private final Object LOCK = new Object();
    private final TimeFormatter timeFormatter;
    private final LogFileHandler logFileHandler;

    private boolean logFileHandlerInitialized = false;

    /**
     * @param timeFormatter used for displaying time, null for no time
     * @param logFileHandler for file handling
     * @param registerShutdownHook to automatically call dispose (and flush log files!) when the application shuts down. Recommended: true.
     */
    public FileLogFunction(TimeFormatter timeFormatter, LogFileHandler logFileHandler, boolean registerShutdownHook) {
        this.timeFormatter = timeFormatter;
        this.logFileHandler = logFileHandler;

        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::dispose));
        }
    }

    private final StringBuilder log_sb = new StringBuilder();
    private final PrintWriter log_sb_writer = new PrintWriter(new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            log_sb.append(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            //NOP
        }

        @Override
        public void close() throws IOException {
            //NOP
        }
    });

    @Override
    public void log(String name, long time, byte level, CharSequence content, Throwable error) {
        synchronized (LOCK) {
            if (!logFileHandlerInitialized) {
                logFileHandler.initialize();
                logFileHandlerInitialized = true;
            }

            final StringBuilder sb = this.log_sb;
            sb.append('[');
            if (timeFormatter != null) {
                timeFormatter.format(time, sb);
                sb.append(' ');
            }
            sb.append(alignedLevelName(level)).append(']').append(' ').append(name).append(':').append(' ');
            sb.append(content).append('\n');
            if (error != null) {
                final PrintWriter writer = this.log_sb_writer;
                error.printStackTrace(writer);
                writer.flush();
            }

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

    public static String alignedLevelName(byte logLevel){
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
