package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.TPLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.Comparator;

/**
 * FileCreationStrategy which returns files with current date/time.
 * It can also, optionally, delete old log files when they take up too much space.
 */
public class DateTimeFileCreationStrategy implements LogFileCreationStrategy {

    public static final @NotNull DateTimeFormatter DEFAULT_DATE_FILE_NAME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .parseDefaulting(ChronoField.NANO_OF_DAY, 0L)
            .toFormatter();

    public static final @NotNull DateTimeFormatter DEFAULT_DATE_TIME_FILE_NAME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('.')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0L)
            .toFormatter();

    public static final @NotNull String DEFAULT_LOG_FILE_EXTENSION = "log";
    public static final long FOLDER_SIZE_LIMIT_NONE = -1L;

    private final @NotNull DateTimeFormatter formatter;
    private final boolean allowAppend;
    private final @NotNull String extension;
    private final long folderKiloByteLimit;
    private final @Nullable TemporalAmount keepLogsAtLeastFor;

    /**
     * @param formatter          to use when creating files. Must not be empty
     * @param allowAppend        whether logs should be appended to existing files or if new files should be created in case of naming conflict
     * @param extension          of the log files (without leading dot)
     * @param folderKBLimit      how many kilobytes should be tolerated inside the log folder before logs are removed, 0 = always remove all other log files, {@link #FOLDER_SIZE_LIMIT_NONE} = never delete. Note that this does not include files which cannot be deleted, i.e. non-log files and files which are too new to be deleted.
     * @param keepLogsAtLeastFor even if the folder is larger than the limit, do not delete logs younger than this
     */
    public DateTimeFileCreationStrategy(@NotNull DateTimeFormatter formatter, boolean allowAppend, @NotNull String extension, long folderKBLimit, @Nullable TemporalAmount keepLogsAtLeastFor) {
        this.formatter = formatter;
        this.allowAppend = allowAppend;
        this.extension = extension.startsWith(".") ? extension.substring(1) : extension;
        this.folderKiloByteLimit = folderKBLimit;
        this.keepLogsAtLeastFor = keepLogsAtLeastFor;
    }

    @Override
    public @NotNull File getLogFile(@NotNull File logDirectory) throws Exception {
        final StringBuilder sb = new StringBuilder();
        formatter.formatTo(TPLogger.getTimeProvider().time(), sb);
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
    public void performCleanup(@NotNull File logDirectory, @NotNull File currentLogFile, @NotNull TPLogger logger) {
        if (folderKiloByteLimit <= FOLDER_SIZE_LIMIT_NONE) {
            // No cleanup, ever
            return;
        }
        final File[] filesInLogFolder = logDirectory.listFiles();
        if (filesInLogFolder == null || filesInLogFolder.length == 0) {
            logger.debug("Cleanup not happening, nothing to cleanup");
            return;
        }

        final ZonedDateTime now = TPLogger.getTimeProvider().time();

        final ArrayList<FileWithTime> deletableFiles = new ArrayList<>();
        considerFile:
        for (File file : filesInLogFolder) {
            if (file.equals(currentLogFile)) continue;
            if (!file.isFile() || file.isHidden()) continue;

            final String fileName = file.getName();

            ZonedDateTime dateTime;
            final ParsePosition parsePosition = new ParsePosition(0);
            try {
                final TemporalAccessor temporalAccessor = formatter.parse(fileName, parsePosition);
                try {
                    dateTime = ZonedDateTime.from(temporalAccessor);
                } catch (DateTimeException ignored) {
                    try {
                        dateTime = LocalDateTime.from(temporalAccessor).atZone(now.getZone());
                    } catch (DateTimeException e) {
                        logger.error("Failed to extract ZonedDateTime from {}. Provided formatter may not hold enough information.", fileName, e);
                        continue;
                    }
                }
            } catch (Exception ex) {
                //Not a log file, probably
                continue;
            }

            int position = parsePosition.getIndex();
            if (position <= 0 || position >= fileName.length() || parsePosition.getErrorIndex() != -1) {
                // Not matched anything or matched whole fileName = not a log file (because we need at least extension)
                continue;
            }
            if (fileName.charAt(position) != '.') {
                // Need a dot for file index or extension, not a log file
                continue;
            }
            position++; // Now after the first dot
            final int extensionAt = fileName.indexOf(extension, position);
            // Is there an extension? (Arbitrary text can appear after extension, for example archival appends ".gz")
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

            if (dateTime.isAfter(now)) {
                logger.warn("While trying to clean up, found log file which is from the future: {} ({} is after {}). Keeping.", file, dateTime, now);
                continue;
            }

            if (keepLogsAtLeastFor == null) {
                deletableFiles.add(new FileWithTime(file, dateTime, fileIndex));
            } else {
                final ZonedDateTime willBeDeletableAtTime = dateTime.plus(keepLogsAtLeastFor);
                if (willBeDeletableAtTime.isBefore(now)) {
                    //Already deletable
                    deletableFiles.add(new FileWithTime(file, dateTime, fileIndex));
                }
            }
        }

        // Oldest first
        deletableFiles.sort(Comparator.naturalOrder());

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
        public final @NotNull File file;
        public final @NotNull ZonedDateTime time;
        public final int index;

        private FileWithTime(@NotNull File file, @NotNull ZonedDateTime time, int index) {
            this.file = file;
            this.time = time;
            this.index = index;
        }

        @Override
        public int compareTo(@NotNull FileWithTime o) {
            final int cmp = time.compareTo(o.time);
            if (cmp == 0) {
                return Integer.compare(index, o.index);
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
