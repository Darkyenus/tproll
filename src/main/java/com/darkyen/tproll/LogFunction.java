package com.darkyen.tproll;

import com.darkyen.tproll.logfunctions.ConsoleLogFunction;
import org.slf4j.Marker;

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

    /** Default log function. It is {@link ConsoleLogFunction()}. */
    LogFunction DEFAULT_LOG_FUNCTION = new ConsoleLogFunction();
}
