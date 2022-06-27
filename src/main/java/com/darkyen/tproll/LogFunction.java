package com.darkyen.tproll;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Marker;

/**
 * Implements logging of the message to a medium, depending on the implementation.
 * For example console, file or other logging system.
 */
public abstract class LogFunction {
    /**
     * Called when logger needs to log a message. Called only when that log level is enabled in the logger.
     * Can be called by any thread, even simultaneously, and thus MUST be thread safe.
     * @param name of the logger
     * @param time in ms since start of the app or since 1970
     * @param level of this message
     * @param marker provided or null
     * @param content of this message, formatted, without trailing newline. Do not keep around!
     */
    public abstract void log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content);

    /**
     * Additional check whether this log function will log message of given level/marker.
     * This is only secondary check, primary level check is done through log level of TPLogger.
     * @return if such message would be logged
     */
    public boolean isEnabled(byte level, @Nullable Marker marker){
        return true;
    }

    /** Called when the {@link LogFunction} is assigned to {@link TPLogger#setLogFunction(LogFunction)} */
    public void start() {}

    /** Called when the {@link LogFunction} is overridden from active by {@link TPLogger#setLogFunction(LogFunction)} or on system shutdown. */
    public void stop() {}
}
