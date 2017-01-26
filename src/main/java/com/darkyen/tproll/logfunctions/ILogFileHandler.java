package com.darkyen.tproll.logfunctions;

/**
 * Interface for file handlers.
 * Log file handlers are an abstraction over ideal log sink, and handle file selection, writing, closing, etc.
 */
public interface ILogFileHandler {

    void initialize();

    void log(CharSequence message);

    void dispose();

}
