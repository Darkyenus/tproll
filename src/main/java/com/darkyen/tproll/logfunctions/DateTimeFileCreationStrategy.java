package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.TPLogger;

import java.io.File;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;

/**
 * FileCreationStrategy which returns files with current date/time.
 * It can also, optionally, delete old log files when they take up too much space.
 */
public class DateTimeFileCreationStrategy implements LogFileCreationStrategy {

    public static final DateTimeFormatter DEFAULT_DATE_FILE_NAME_FORMATTER = new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .toFormatter();

    public static final DateTimeFormatter DEFAULT_DATE_TIME_FILE_NAME_FORMATTER = new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4)
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
            .toFormatter();

    public static final String DEFAULT_LOG_FILE_EXTENSION = ".log";

    private final DateTimeFormatter formatter;
    private final boolean allowAppend;
    private final String extension;
    private final long folderByteLimit;
    private final TemporalAmount keepLogsAtLeastFor;

    public DateTimeFileCreationStrategy(DateTimeFormatter formatter, boolean allowAppend, String extension, long folderKBLimit, TemporalAmount keepLogsAtLeastFor) {
        this.formatter = formatter;
        this.allowAppend = allowAppend;
        this.extension = extension;
        this.folderByteLimit = folderKBLimit * 1024;
        this.keepLogsAtLeastFor = keepLogsAtLeastFor;
    }

    @Override
    public File getLogFile(File logDirectory) throws Exception {
        final StringBuilder sb = new StringBuilder();
        formatter.formatTo(TPLogger.getTimeProvider().time(), sb);
        final int lengthBeforeExtension = sb.length();
        sb.append(extension);

        final String[] existingFiles = logDirectory.list();
        int nextFileNumber = 2;
        String currentName = sb.toString();

        while (true) {
            fileExists:
            {
                if (existingFiles != null) {
                    for (String file : existingFiles) {
                        if (file.startsWith(currentName)) {
                            // Such file exists
                            break fileExists;
                        }
                    }
                } else {
                    // Pessimistic fallback
                    if (new File(logDirectory, currentName).exists()) {
                        break fileExists;
                    }
                }
                //No such file exists! We can use it.
                break;
            }
            // Try different file, if not exhausted
            sb.setLength(lengthBeforeExtension);
            sb.append('.').append(nextFileNumber).append(extension);
            currentName = sb.toString();
            nextFileNumber++;
            if (nextFileNumber >= 10_000) {
                //Can't find anything that does not exist
                throw new Exception("Failed to create log file, all variants exist.");
            }
        }
        return new File(logDirectory, currentName);
    }

    @Override
    public void performCleanup(File logDirectory, File currentLogFile, TPLogger logger) {
        if (folderByteLimit <= 0) return;
        final File[] filesInLogFolder = logDirectory.listFiles();
        if (filesInLogFolder == null || filesInLogFolder.length == 0) {
            logger.debug("Cleanup not happening, nothing to cleanup");
            return;
        }

        final ZonedDateTime now = TPLogger.getTimeProvider().time();

        final ArrayList<FileWithTime> deletableFiles = new ArrayList<>();
        for (File file : filesInLogFolder) {
            if (file.equals(currentLogFile)) continue;
            if (!file.isFile() || file.isHidden()) continue;

            try {
                final ParsePosition position = new ParsePosition(0);
                final String fileName = file.getName();
                final TemporalAccessor dateTime = formatter.parse(fileName, position);
                if (fileName.indexOf(extension, position.getIndex()) == -1) {
                    //Extension is missing, not a log file
                    continue;
                }

                ZonedDateTime from;
                try {
                    from = ZonedDateTime.from(dateTime);
                } catch (DateTimeException ex) {
                    final LocalDateTime localFrom = LocalDateTime.from(dateTime);
                    from = ZonedDateTime.of(localFrom, now.getZone());
                }

                if (from.isAfter(now)) {
                    logger.warn("While trying to clean up, found log file which is from the future: {}. Aborting cleanup.", file);
                    continue;
                }

                if (keepLogsAtLeastFor == null) {
                    deletableFiles.add(new FileWithTime(file, from));
                } else {
                    final ZonedDateTime willBeDeletableAtTime = from.plus(keepLogsAtLeastFor);
                    if (willBeDeletableAtTime.isBefore(now)) {
                        //Already deletable
                        deletableFiles.add(new FileWithTime(file, from));
                    }
                }
            } catch (DateTimeException ex) {
                //Not a log file, probably
            }
        }

        deletableFiles.sort(FileWithTime::compareTo);
        //TODO Assume first is newest
        long totalSizeBytes = 0;
        int keep;
        for (keep = 0; keep < deletableFiles.size(); keep++) {
            totalSizeBytes += deletableFiles.get(keep).file.length();
            if (totalSizeBytes > folderByteLimit) {
                break;
            }
        }
        long deletedBytes = 0;
        for (int delete = keep; delete < deletableFiles.size(); delete++) {
            final File fileToDelete = deletableFiles.get(delete).file;
            deletedBytes += fileToDelete.length();
            logger.log(TPLogger.LOG, null, "Deleting old log file over size limit: {}", fileToDelete);
            if(!fileToDelete.delete()) {
                logger.warn("Old log file not deleted!");
            }
        }
        if(deletableFiles.size() != keep) {
            logger.log(TPLogger.LOG, null, "Deleted {} old log files with total size of {} bytes", deletableFiles.size() - keep, deletedBytes);
        }
    }

    @Override
    public boolean shouldAppend() {
        return allowAppend;
    }

    private static final class FileWithTime implements Comparable<FileWithTime> {
        public final File file;
        public final ZonedDateTime time;

        private FileWithTime(File file, ZonedDateTime time) {
            this.file = file;
            this.time = time;
        }

        @Override
        public int compareTo(FileWithTime o) {
            if (time.isEqual(o.time)) return 0;
            else if (time.isBefore(o.time)) return 1;
            else return -1;
        }
    }
}
