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

    public static final String DEFAULT_LOG_FILE_EXTENSION = "log";
    public static final long FOLDER_SIZE_LIMIT_ZERO = 0L;
    public static final long FOLDER_SIZE_LIMIT_NONE = -1L;

    private final DateTimeFormatter formatter;
    private final boolean allowAppend;
    private final String extension;
    private final long folderKiloByteLimit;
    private final ReadableDuration keepLogsAtLeastFor;

    /**
     * @param formatter          to use when creating files. Must not be empty
     * @param allowAppend        whether logs should be appended to existing files or if new files should be created in case of naming conflict
     * @param extension          of the log files (without leading dot)
     * @param folderKBLimit      how many kilobytes should be tolerated inside the log folder before logs are removed, {@link #FOLDER_SIZE_LIMIT_ZERO} = always remove all other log files, {@link #FOLDER_SIZE_LIMIT_NONE} = never delete. Note that this does not include files which cannot be deleted, i.e. non-log files and files which are too new to be deleted.
     * @param keepLogsAtLeastFor even if the folder is larger than the limit, do not delete logs younger than this
     */
    public DateTimeFileCreationStrategy(DateTimeFormatter formatter, boolean allowAppend, String extension, long folderKBLimit, ReadableDuration keepLogsAtLeastFor) {
        this.formatter = formatter;
        this.allowAppend = allowAppend;
        this.extension = extension.startsWith(".") ? extension.substring(1) : extension;
        this.folderKiloByteLimit = folderKBLimit;
        this.keepLogsAtLeastFor = keepLogsAtLeastFor;
    }

    @Override
    public File getLogFile(File logDirectory) throws Exception {
        final StringBuilder sb = new StringBuilder();
        formatter.printTo(sb, TPLogger.getTimeProvider().time());
        sb.append('.');
        final int lengthBeforeSuffix = sb.length();
        sb.append(extension);

        String currentName = sb.toString();
        if (allowAppend) {
            // If appending is allowed, just return existing file
            return new File(logDirectory, currentName);
        }

        final String[] existingFiles = logDirectory.list();
        if (existingFiles == null) {
            // Directory (hopefully) does not exist, so default filename is definitely safe
            return new File(logDirectory, currentName);
        }
        int nextFileNumber = 2;

        while (true) {
            fileExists:
            {
                for (String file : existingFiles) {
                    if (file.startsWith(currentName)) {
                        // Such file exists
                        break fileExists;
                    }
                }
                //No such file exists! We can use it.
                break;
            }
            // Try different file, if not exhausted
            sb.setLength(lengthBeforeSuffix);
            sb.append(nextFileNumber).append('.').append(extension);
            currentName = sb.toString();
            nextFileNumber++;
            if (nextFileNumber >= 10000) {
                //Can't find anything that does not exist
                throw new Exception("Failed to create log file, all variants exist");
            }
        }
        return new File(logDirectory, currentName);
    }

    @Override
    public void performCleanup(File logDirectory, File currentLogFile, TPLogger logger) {
        if (folderKiloByteLimit <= FOLDER_SIZE_LIMIT_NONE) {
            // No cleanup, ever
            return;
        }
        final File[] filesInLogFolder = logDirectory.listFiles();
        if (filesInLogFolder == null || filesInLogFolder.length == 0) {
            logger.debug("Cleanup not happening, nothing to cleanup");
            return;
        }

        final DateTime now = TPLogger.getTimeProvider().time();

        final ArrayList<FileWithTime> deletableFiles = new ArrayList<FileWithTime>();
        considerFile:
        for (File file : filesInLogFolder) {
            if (file.equals(currentLogFile)) continue;
            if (!file.isFile() || file.isHidden()) continue;

            final String fileName = file.getName();
            final MutableDateTime mutableDateTime = new MutableDateTime();

            int position;
            try {
                mutableDateTime.setMillis(now);
                position = formatter.parseInto(mutableDateTime, fileName, 0);
            } catch (UnsupportedOperationException ex) {
                //Not a log file, probably
                continue;
            } catch (IllegalArgumentException ex) {
                //Not a log file, probably
                continue;
            }

            if (position <= 0 || position >= fileName.length()) {
                // Not matched anything or matched whole fileName = not a log file (because we need at least extension)
                continue;
            }
            if (fileName.charAt(position) != '.') {
                // Need a dot for file index or extension, not a log file
                continue;
            }
            position++; // Now after the first dot
            final int extensionAt = fileName.indexOf(extension, position);
            // Is there an extension? (Arbitrary text can appear after extension, for example archival appends ".gzip")
            if (extensionAt < position) {
                // Need an extension, not a log file
                continue;
            }

            final int fileIndex;
            if (extensionAt > position) {
                // It has an file index
                final int extensionDot = extensionAt - 1;
                if (fileName.charAt(extensionDot) != '.') {
                    // Need a dot between file index and log extension
                    continue;
                }

                int foundFileIndex = 0;
                while (position < extensionDot) {
                    final int digit = Character.digit(fileName.charAt(position), 10);
                    if (digit == -1) {
                        // All characters in the file index must be digits
                        continue considerFile;
                    }
                    foundFileIndex *= 10;
                    foundFileIndex += digit;
                    position++;
                }
                fileIndex = foundFileIndex;
            } else {
                fileIndex = 1;
            }

            if (mutableDateTime.isAfter(now)) {
                logger.warn("While trying to clean up, found log file which is from the future: {} ({} is after {}). Keeping.", file, mutableDateTime, now);
                continue;
            }

            final DateTime dateTime = mutableDateTime.toDateTime();

            if (keepLogsAtLeastFor == null) {
                deletableFiles.add(new FileWithTime(file, dateTime, fileIndex));
            } else {
                final DateTime willBeDeletableAtTime = dateTime.plus(keepLogsAtLeastFor);
                if (willBeDeletableAtTime.isBefore(now)) {
                    //Already deletable
                    deletableFiles.add(new FileWithTime(file, dateTime, fileIndex));
                }
            }
        }

        // Oldest first
        Collections.sort(deletableFiles);

        // Go from newest to oldest, stop when the total size is too big
        long totalSizeBytes = 0;
        int deleted = deletableFiles.size() - 1;
        while (deleted > 0) {
            totalSizeBytes += deletableFiles.get(deleted - 1).file.length();
            if (totalSizeBytes > folderKiloByteLimit * 1000) {
                // This file triggered the culling, it will be deleted
                break;
            }
            // This file is fine, keep it
            deleted--;
        }

        // Now remove all files which didn't make the cut [0-keep]
        long deletedBytes = 0;
        for (int i = 0; i < deleted; i++) {
            final File fileToDelete = deletableFiles.get(i).file;
            deletedBytes += fileToDelete.length();
            logger.log(TPLogger.LOG, null, "Deleting old log file over size limit: {}", fileToDelete);
            if(!fileToDelete.delete()) {
                logger.warn("Old log file not deleted!");
            }
        }

        if(deleted > 0) {
            logger.log(TPLogger.LOG, null, "Deleted {} old log files with total size of {} bytes", deleted, deletedBytes);
        }
    }

    @Override
    public boolean shouldAppend() {
        return allowAppend;
    }

    private static final class FileWithTime implements Comparable<FileWithTime> {
        public final File file;
        public final DateTime time;
        public final int index;

        private FileWithTime(File file, DateTime time, int index) {
            this.file = file;
            this.time = time;
            this.index = index;
        }

        @Override
        public int compareTo(FileWithTime o) {
            final int cmp = time.compareTo(o.time);
            if (cmp == 0) {
                // Inlined: Integer.compare(index, o.index);
                return (index < o.index) ? -1 : ((index == o.index) ? 0 : 1);
            }
            return cmp;
        }

        @Override
        public String toString() {
            return "FileWithTime{" +
                    "file=" + file +
                    ", time=" + time +
                    ", index=" + index +
                    '}'+'\n';
        }
    }
}
