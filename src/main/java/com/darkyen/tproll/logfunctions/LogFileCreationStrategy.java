package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.TPLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Handles file choosing and cleanup for {@link LogFileHandler}. File janitor.
 *
 * @see DateTimeFileCreationStrategy for default implementation
 */
public interface LogFileCreationStrategy {

    /** Select a file from given directory to log to. The file may or may not exist yet. */
    @NotNull File getLogFile(@NotNull File logDirectory) throws Exception;

    /** After the file is created, old files may need to be removed or otherwise handled.
     * This should do it, if applicable.
     * @param logDirectory in which logs are saved
     * @param currentLogFile to which we are currently logging - DON'T TOUCH IT HERE!
     * @param logger for internal logging, use "LOG" message level for important messages */
    void performCleanup(@NotNull File logDirectory, @NotNull File currentLogFile, @NotNull TPLogger logger);

    /** Whether or not is the file returned by {@link #getLogFile(File)} meant for appending or overwriting. */
    boolean shouldAppend();

}
