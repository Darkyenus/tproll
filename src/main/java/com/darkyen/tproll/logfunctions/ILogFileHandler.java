package com.darkyen.tproll.logfunctions;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Marker;

/**
 * Actual file writing inside {@link FileLogFunction} is delegated to implementations of this interface.
 * This is an abstraction over ideal log sink, the implementation must handle file selection, writing, closing, etc.,
 * though it is not limited only to files.
 *
 * Methods may be called from arbitrary thread, but always mutually exclusively.
 */
public interface ILogFileHandler {

    /** Called before first {@link #log(CharSequence)} invocation */
    void initialize();

    /** Called by {@link FileLogFunction#log(String, long, byte, Marker, CharSequence)} with the message which should
     * get logged. */
    void log(@NotNull CharSequence message);

    /** Called by {@link FileLogFunction#dispose()}. */
    void dispose();

}
