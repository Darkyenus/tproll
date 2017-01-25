package com.darkyen.tproll.advanced;

/**
 *
 */
public interface LogFileHandler {

    void initialize();

    void log(CharSequence message);

    void dispose();

}
