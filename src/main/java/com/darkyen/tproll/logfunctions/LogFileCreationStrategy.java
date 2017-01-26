package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.TPLogger;
import org.slf4j.Logger;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 *
 */
public interface LogFileCreationStrategy {

    File getLogFile(File logFolder) throws Exception;

    void performCleanup(File logFolder, File currentLogFile, TPLogger logger);

    boolean shouldAppend();

    static LogFileCreationStrategy createDefaultDateStrategy() {
        return new DateTimeFileCreationStrategy(DateTimeFileCreationStrategy.DEFAULT_DATE_FILE_NAME_FORMATTER,
                true,
                DateTimeFileCreationStrategy.DEFAULT_LOG_FILE_EXTENSION,
                1024*512, // 0.5 GB of logs
                Duration.ofDays(30));
    }
}
