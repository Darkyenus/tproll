package com.darkyen.tproll.logfunctions;

/**
 *
 */
public interface LogFileHandler {

    void initialize();

    void log(CharSequence message);

    void dispose();

}
