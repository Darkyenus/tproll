package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.TPLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.concurrent.ForkJoinPool;
import java.util.zip.GZIPOutputStream;

/**
 * Handles logging into a file.
 */
public class LogFileHandler implements ILogFileHandler {

    private static final @NotNull DateTimeFormatter FILE_ACTION_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral(' ')
            .appendZoneOrOffsetId()
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();

    private final TPLogger LOG = new TPLogger("LogFileHandler");

    /** Used as a synchronization lock for opening/closing/cleanup */
    private final @NotNull File logDirectory;
    private final @NotNull LogFileCreationStrategy fileCreationStrategy;
    private final boolean compressOnExit;
    private final long reservedFilesystemBytes;
    private final long maxFileSize;
    private final boolean flush;

    /**
     * Encompasses an opened file stream, implements buffering and written/remaining byte counting.
     */
    private static final class OpenedFile extends OutputStream {
        final @NotNull File file;
        private final @NotNull FileOutputStream outputStream;
        /** Use this to actually write the data out. */
        final @NotNull Writer writer = new OutputStreamWriter(this, StandardCharsets.UTF_8);

        private final byte[] buffer = new byte[8192];
        private int bufferFilled = 0;

        private long fileSize;

        private static final long CHECK_REMAINING_INTERVAL_MS = 60_000;
        private long nextRemainingCheckInMs = 0L;
        private long remainingFileSystemBytes;

        boolean cleanupAttempted = false;
        boolean notEnoughSpaceLogged = false;

        OpenedFile(@NotNull File file, @NotNull FileOutputStream stream) {
            this.file = file;
            this.fileSize = file.length();
            this.outputStream = stream;
        }

        public long fileSize() {
            return fileSize + bufferFilled;
        }

        public long remainingDestinationCapacity(boolean forceCheck) {
            final long now = System.currentTimeMillis();
            if (forceCheck || nextRemainingCheckInMs <= now) {
                nextRemainingCheckInMs = now + CHECK_REMAINING_INTERVAL_MS;
                remainingFileSystemBytes = file.getFreeSpace();
            }
            return remainingFileSystemBytes - bufferFilled;
        }

        @Override
        public void write(byte @NotNull [] b) throws IOException {
            this.write(b, 0, b.length);
        }

        @Override
        public void write(int b) throws IOException {
            if (bufferFilled >= buffer.length) {
                flushBuffer();
            }
            buffer[bufferFilled++] = (byte) b;
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) throws IOException {
            while (true) {
                int remainingBufferCapacity = buffer.length - bufferFilled;
                final int writeNow = Math.min(len, remainingBufferCapacity);
                System.arraycopy(b, off, buffer, bufferFilled, writeNow);
                len -= writeNow;
                off += writeNow;
                bufferFilled += writeNow;

                if (len > 0) {
                    // Buffer is full, flush and write more
                    flushBuffer();
                } else {
                    // Done
                    break;
                }
            }
        }

        private void flushBuffer() throws IOException {
            outputStream.write(buffer, 0, bufferFilled);
            fileSize += bufferFilled;
            remainingFileSystemBytes -= bufferFilled;
            bufferFilled = 0;
        }

        @Override
        public void flush() throws IOException {
            flushBuffer();
            outputStream.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                flushBuffer();
            } finally {
                outputStream.close();
            }
        }
    }

    private boolean started = false;
    private @Nullable OpenedFile opened = null;

    /**
     * @param logDirectory            in which the log files should be created
     * @param fileCreationStrategy    how the files in logDirectory should be created
     * @param compressOnExit          whether the log files should be compressed when they are closed
     * @param reservedFilesystemBytes do not log any more if the filesystem has less than this many free bytes
     * @param maxFileSize             start logging into a new file when the size reaches this number
     * @param flush                   flush after each log? False may lead to slightly better performance when logging a large amount of small messages, but the last file entry may not be complete and messages may be lost if the JVM crashes.
     */
    public LogFileHandler(
            @NotNull File logDirectory,
            @NotNull LogFileCreationStrategy fileCreationStrategy,
            boolean compressOnExit,
            long reservedFilesystemBytes,
            long maxFileSize, boolean flush) {
        this.logDirectory = logDirectory;
        this.fileCreationStrategy = fileCreationStrategy;
        this.compressOnExit = compressOnExit;
        this.reservedFilesystemBytes = reservedFilesystemBytes;
        this.maxFileSize = maxFileSize;
        this.flush = flush;
    }

    private @Nullable OpenedFile openFile() {
        synchronized (logDirectory) {
            try {
                final File logFile = fileCreationStrategy.getLogFile(logDirectory, maxFileSize);

                //Verify that the file is valid
                //noinspection ConstantConditions
                if (logFile == null) {
                    throw new NullPointerException("File creation strategy returned null");
                } else if (logFile.isDirectory()) {
                    throw new FileNotFoundException("Returned log file at '" + logFile.getAbsolutePath() + "' is a directory");
                } else {
                    final File parentFile = logFile.getParentFile();
                    if (parentFile == null) {
                        throw new FileNotFoundException("'" + logFile + "' has no parent");
                    }
                    if (parentFile.exists()) {
                        if (!parentFile.isDirectory()) {
                            throw new FileNotFoundException("Parent file of '" + logFile.getAbsolutePath() + "' exists and is not a directory");
                        }
                    } else {
                        if (!parentFile.mkdirs() && !parentFile.isDirectory()) {
                            throw new IOException("Failed to create parent directories for '" + logFile.getAbsolutePath() + "'");
                        }
                    }
                }

                if (logFile.isFile()) {
                    //It already exists, we will override or append
                    if (!logFile.canWrite()) {
                        throw new IllegalStateException("Returned file can't be written to");
                    }
                } else {
                    if (logFile.exists()) {
                        throw new IllegalArgumentException("Returned file at '" + logFile.getAbsolutePath() + "' is not a file but exists");
                    }
                }

                final FileOutputStream stream = new FileOutputStream(logFile, fileCreationStrategy.shouldAppend());
                boolean success = false;
                try {
                    final OpenedFile openedFile = new OpenedFile(logFile, stream);
                    final Writer writer = openedFile.writer;

                    writer.append("Log file opened at ");
                    FILE_ACTION_TIME_FORMATTER.formatTo(TPLogger.getTimeProvider().time(), writer);
                    writer.append('\n');
                    writer.flush();

                    ForkJoinPool.commonPool().execute(() -> cleanup(logFile));
                    success = true;
                    return openedFile;
                } finally {
                    if (!success) {
                        stream.close();
                    }
                }
            } catch (Throwable t) {
                LOG.error("Failed to open a file", t);
                return null;
            }
        }
    }

    private void cleanup(File currentLogFile) {
        synchronized (logDirectory) {
            try {
                fileCreationStrategy.performCleanup(logDirectory, currentLogFile, LOG);
            } catch (Throwable t) {
                LOG.error("Cleanup failed");
            }
        }
    }

    private void closeFile(@NotNull OpenedFile file, @NotNull String reason) {
        synchronized (logDirectory) {
            try {
                final Writer w = file.writer;
                w.append("Log file closed at ");
                FILE_ACTION_TIME_FORMATTER.formatTo(TPLogger.getTimeProvider().time(), w);
                w.append(" (");
                w.append(reason);
                w.append(")\n");
            } catch (Throwable t) {
                LOG.error("Failed to write file {} footer", file.file, t);
            }

            try {
                file.writer.close();
            } catch (Throwable t) {
                LOG.error("Failed to close file {}", file.file, t);
            }

            if (compressOnExit) {
                final File openedFile = file.file;
                final File compressedFile = new File(openedFile.getParentFile(), openedFile.getName() + ".gz");
                final long originalSize = openedFile.length();
                if (!compressedFile.exists() && originalSize > 0) {
                    try (FileInputStream in = new FileInputStream(openedFile)) {
                        try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(compressedFile))) {
                            final byte[] buffer = new byte[(int) Math.min(8192, originalSize)];
                            while (true) {
                                final int read = in.read(buffer);
                                if (read <= 0) break;
                                out.write(buffer, 0, read);
                            }
                        }
                    } catch (IOException e) {
                        LOG.error("Failed to compress {}", openedFile);
                    }

                    final long compressedSize = compressedFile.length();
                    if (compressedSize <= 0) {
                        LOG.error("Failed to compress {}, result file is empty", openedFile);
                    } else {
                        final File delete = (compressedSize < originalSize) ? openedFile : compressedFile;

                        if (!delete.delete() && delete.isFile()) {
                            LOG.warn("Failed to delete {} after log compression", delete);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void start() {
        this.opened = openFile();
        started = true;
    }

    @Override
    public void stop() {
        started = false;
        final OpenedFile opened = this.opened;
        this.opened = null;
        if (opened != null) {
            closeFile(opened, "shutdown");
        }
    }

    @Override
    public boolean log(@NotNull CharSequence message) {
        OpenedFile opened = this.opened;
        if (opened == null && (!started || (this.opened = opened = openFile()) == null)) {
            return false;
        }

        final Writer fileWriter = opened.writer;

        if (reservedFilesystemBytes > 0 && opened.remainingDestinationCapacity(false) < reservedFilesystemBytes) {
            final boolean overCapacity;
            // The capacity is full.
            if (!opened.cleanupAttempted) {
                opened.cleanupAttempted = true;
                cleanup(opened.file);
                overCapacity = opened.remainingDestinationCapacity(true) < reservedFilesystemBytes;
            } else {
                overCapacity = true;
            }

            if (overCapacity) {
                // Print and fail, don't close
                if (!opened.notEnoughSpaceLogged) {
                    opened.notEnoughSpaceLogged = true;
                    try {
                        fileWriter.append("<filesystem capacity exhausted>\n");
                        fileWriter.flush();
                    } catch (IOException e) {
                        LOG.error("Failed to write filesystem-over-capacity warning", e);
                    }
                }
                return false;
            } else {
                opened.cleanupAttempted = false;
                opened.notEnoughSpaceLogged = false;
            }
        }

        try {
            fileWriter.append(message);
            if (flush) {
                fileWriter.flush();
            }
        } catch (IOException e) {
            LOG.error("Failed to write {}", opened.file, e);
            closeFile(opened, "failure to write");
            this.opened = null;
            return false;
        }

        if (maxFileSize > 0 && maxFileSize < Long.MAX_VALUE) {
            if (opened.fileSize() > maxFileSize) {
                this.opened = null;
                closeFile(opened, "file too large");
            }
        }

        return true;
    }
}
