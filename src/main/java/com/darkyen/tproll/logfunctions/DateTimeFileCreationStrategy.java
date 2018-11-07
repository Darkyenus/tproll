package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.TPLogger;
import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDuration;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * FileCreationStrategy which returns files with current date/time.
 * It can also, optionally, delete old log files when they take up too much space.
 */
public class DateTimeFileCreationStrategy implements LogFileCreationStrategy {

    public static final DateTimeFormatter DEFAULT_DATE_FILE_NAME_FORMATTER = new DateTimeFormatterBuilder()
            .appendYear(4, 4)
            .appendLiteral('-')
            .appendMonthOfYear(2)
            .appendLiteral('-')
            .appendDayOfMonth(2)
            .toFormatter();

    public static final DateTimeFormatter DEFAULT_DATE_TIME_FILE_NAME_FORMATTER = new DateTimeFormatterBuilder()
            .appendYear(4, 4)
            .appendLiteral('-')
            .appendMonthOfYear(2)
            .appendLiteral('-')
            .appendDayOfMonth(2)
            .appendLiteral('.')
            .appendHourOfDay(2)
            .appendLiteral('-')
            .appendMinuteOfHour(2)
            .appendLiteral('-')
            .appendSecondOfMinute(2)
            .toFormatter();

    public static final String DEFAULT_LOG_FILE_EXTENSION = ".log";

    private final DateTimeFormatter formatter;
    private final boolean allowAppend;
    private final String extension;
    private final long folderByteLimit;
    private final ReadableDuration keepLogsAtLeastFor;

    public DateTimeFileCreationStrategy(DateTimeFormatter formatter, boolean allowAppend, String extension, long folderKBLimit, ReadableDuration keepLogsAtLeastFor) {
        this.formatter = formatter;
        this.allowAppend = allowAppend;
        this.extension = extension;
        this.folderByteLimit = folderKBLimit * 1024;
        this.keepLogsAtLeastFor = keepLogsAtLeastFor;
    }

    @Override
    public File getLogFile(File logDirectory) throws Exception {
        final StringBuilder sb = new StringBuilder();
        formatter.printTo(sb, TPLogger.getTimeProvider().time());
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
            if (nextFileNumber >= 10000) {
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

        final DateTime now = TPLogger.getTimeProvider().time();

        final ArrayList<FileWithTime> deletableFiles = new ArrayList<FileWithTime>();
        for (File file : filesInLogFolder) {
            if (file.equals(currentLogFile)) continue;
            if (!file.isFile() || file.isHidden()) continue;

            final String fileName = file.getName();
            final MutableDateTime mutableDateTime = new MutableDateTime();
            try {
                final int positionAfterParse = formatter.parseInto(mutableDateTime, fileName, 0);
                if (positionAfterParse < 0) {
                    // Not a log file, probably
                    continue;
                }

                if (fileName.indexOf(extension, positionAfterParse) == -1) {
                    //Extension is missing, not a log file
                    continue;
                }
            } catch (UnsupportedOperationException ex) {
                //Not a log file, probably
                continue;
            } catch (IllegalArgumentException ex) {
                //Not a log file, probably
                continue;
            }

            if (mutableDateTime.isAfter(now)) {
                logger.warn("While trying to clean up, found log file which is from the future: {}. Aborting cleanup.", file);
                continue;
            }

            final DateTime dateTime = mutableDateTime.toDateTime();

            if (keepLogsAtLeastFor == null) {
                deletableFiles.add(new FileWithTime(file, dateTime));
            } else {
                final DateTime willBeDeletableAtTime = dateTime.plus(keepLogsAtLeastFor);
                if (willBeDeletableAtTime.isBefore(now)) {
                    //Already deletable
                    deletableFiles.add(new FileWithTime(file, dateTime));
                }
            }
        }

        Collections.sort(deletableFiles);
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
        public final DateTime time;

        private FileWithTime(File file, DateTime time) {
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
