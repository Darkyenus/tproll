package com.darkyen.tproll.logfunctions;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 *
 */
public interface LogFileCreationStrategy {

    File getLogFile(File logFolder) throws Exception;

    boolean shouldAppend();

    static LogFileCreationStrategy createSimpleDateStrategy() {
        return createDateTimeBasedStrategy(new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .toFormatter(), true, ".log");
    }

    static LogFileCreationStrategy createSimpleDateTimeStrategy() {
        return createDateTimeBasedStrategy(new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .appendLiteral('.')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.SECOND_OF_MINUTE)
                .toFormatter(), false, ".log");
    }

    static LogFileCreationStrategy createDateTimeBasedStrategy(DateTimeFormatter formatter, boolean allowAppend, String extension) {
        return new LogFileCreationStrategy() {
            @Override
            public File getLogFile(File logFolder) throws Exception {
                final StringBuilder sb = new StringBuilder();
                final LocalDateTime now = LocalDateTime.now();

                formatter.formatTo(now, sb);
                final int dateLength = sb.length();

                sb.append(extension);

                File logFile = new File(logFolder, sb.toString());
                if(logFile.exists()){
                    //Need to try appendages
                    tryAppendages: {
                        for (int i = 2; i < 1000; i++) {
                            sb.setLength(dateLength);
                            sb.append('.').append(i).append(extension);
                            logFile = new File(logFolder, sb.toString());
                            if(!logFile.exists()) break tryAppendages;
                        }
                        //Failed to find non-existing file
                        throw new Exception("Failed to create log file, all variants exist.");
                    }
                }

                return logFile;
            }

            @Override
            public boolean shouldAppend() {
                return allowAppend;
            }
        };
    }
}
