package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.util.TimeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.zip.GZIPOutputStream;

/**
 *
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

    private final @NotNull File logDirectory;
    private final @NotNull LogFileCreationStrategy fileCreationStrategy;
    private final boolean compressOnExit;
    private final long reservedFilesystemBytes;

    private @Nullable File openedFile = null;
    private @Nullable PrintWriter fileWriter = null;

    private long remainingBytes = 0L;

    /**
     * @param logDirectory in which the log files should be created
     * @param fileCreationStrategy how the files in logDirectory should be created
     * @param compressOnExit whether the log files should be compressed when they are closed
     * @param reservedFilesystemBytes do not log any more if the filesystem has less than this many free bytes
     */
    public LogFileHandler(@NotNull File logDirectory, @NotNull LogFileCreationStrategy fileCreationStrategy, boolean compressOnExit, long reservedFilesystemBytes) {
        this.logDirectory = logDirectory;
        this.fileCreationStrategy = fileCreationStrategy;
        this.compressOnExit = compressOnExit;
        this.reservedFilesystemBytes = reservedFilesystemBytes;
    }

    @Override
    public void initialize() {
        PrintWriter fileWriter;
        try {
            final File logFile = fileCreationStrategy.getLogFile(logDirectory);

            //Verify that the file is valid
	        //noinspection ConstantConditions
	        if(logFile == null) {
                throw new NullPointerException("File creation strategy returned null");
            } else if(logFile.isDirectory()) {
                throw new FileNotFoundException("Returned log file at '" + logFile.getAbsolutePath() + "' is a directory");
            } else {
                final File parentFile = logFile.getParentFile();
                if(parentFile != null){
                    if(parentFile.exists()){
                        if(!parentFile.isDirectory()){
                            throw new FileNotFoundException("Parent file of '"+logFile.getAbsolutePath()+"' exists and is not a directory");
                        }
                    }else{
                        if(!parentFile.mkdirs()){
                            throw new IOException("Failed to create parent directories for '"+logFile.getAbsolutePath()+"'");
                        }
                    }
                }
            }

            if(logFile.isFile()){
                //It already exists, we will override or append
                if(!logFile.canWrite()){
                    throw new IllegalStateException("Returned file can't be written to");
                }
            }else{
                if(logFile.exists()){
                    throw new IllegalArgumentException("Returned file at '"+logFile.getAbsolutePath()+"' is not a file but exists");
                }
            }

            fileWriter = new PrintWriter(new FileWriter(logFile, fileCreationStrategy.shouldAppend()), true);

            fileWriter.append("Log file opened at ");
            FILE_ACTION_TIME_FORMATTER.formatTo(TPLogger.getTimeProvider().time(), fileWriter);
            fileWriter.append('\n');
            fileWriter.flush();

            this.fileWriter = fileWriter;
            this.openedFile = logFile;

            fileCreationStrategy.performCleanup(logDirectory, logFile, LOG);
        } catch (Exception e) {
            logInternalError("Log file creation failed, being System.err only.", e);
        }
    }

    private static final int CAPACITY_CHECK_EACH_BYTES = 1000_000;//1MB
    private int nextCapacityCheckInBytes = 0;
    private long nextCapacityCheckInMs = System.currentTimeMillis();

    private static final byte CAPACITY_OK = 0;
    private static final byte CAPACITY_FULL = 1;
    private static final byte CAPACITY_JUST_FILLED = 2;

    private byte hasCapacity(int bytesWritten) {
        final File openedFile = this.openedFile;
        if (openedFile == null) {
            return CAPACITY_FULL;
        }

        if (reservedFilesystemBytes <= 0) {
            return CAPACITY_OK;
        }

        nextCapacityCheckInBytes -= bytesWritten;
        if (nextCapacityCheckInBytes <= 0 || System.currentTimeMillis() >= nextCapacityCheckInMs) {
            nextCapacityCheckInBytes = CAPACITY_CHECK_EACH_BYTES;
            nextCapacityCheckInMs = System.currentTimeMillis() + 10000L;

            final boolean hadRemainingBytes = remainingBytes > 0;
            remainingBytes = Math.max(openedFile.getFreeSpace() - reservedFilesystemBytes, 0L);
            final boolean hasRemainingBytes = remainingBytes > 0;
            if (hadRemainingBytes && !hasRemainingBytes) {
                // No more space, logging will stop after this, log one last message (so that we have a timestamp)
                return CAPACITY_JUST_FILLED;
            }
            return hasRemainingBytes ? CAPACITY_OK : CAPACITY_FULL;
        }
        return remainingBytes > 0 ? CAPACITY_OK : CAPACITY_FULL;
    }

    @Override
    public void log(@NotNull CharSequence message) {
        final PrintWriter fileWriter = this.fileWriter;
        if (fileWriter != null) {
            final byte cap = hasCapacity(message.length() /*chars != bytes, but close enough, most of the time it will be ascii anyway*/);
            if (cap == CAPACITY_FULL) {
                return;
            }
            fileWriter.append(message);
            if (cap == CAPACITY_JUST_FILLED) {
                fileWriter.print("<filesystem capacity exhausted>\n");
                fileWriter.flush();
            }
        } else {
            System.err.append("com.darkyen.tproll.advanced.LogFileHandler: broken, using stderr:\n");
            System.err.append(message);
        }
    }

    @Override
    public void dispose() {
        final PrintWriter fileWriter = this.fileWriter;
        final File openedFile = this.openedFile;
        this.fileWriter = null;
        this.openedFile = null;

        if(fileWriter != null){
            fileWriter.append("Log file closed at ");
			try {
				FILE_ACTION_TIME_FORMATTER.formatTo(TPLogger.getTimeProvider().time(), fileWriter);
			} catch (Exception e) {
				System.err.println("Closing timestamp printing failed");
				e.printStackTrace(System.err);
				fileWriter.append("<failed to print>");
			}
			fileWriter.append('\n');
            fileWriter.flush();

            if(fileWriter.checkError()){
                logInternalError("FileWriter has encountered an unknown error (in dispose())", null);
            }
            fileWriter.close();


            if (compressOnExit && openedFile != null) {
                final File compressedFile = new File(openedFile.getParentFile(), openedFile.getName()+".gz");
                if (!compressedFile.exists()) {
					GZIPOutputStream out = null;
					FileInputStream in = null;
					try {
						out = new GZIPOutputStream(new FileOutputStream(compressedFile));
						in = new FileInputStream(openedFile);

						final byte[] buffer = new byte[(int)Math.min(4096, openedFile.length())];
						while (true) {
							final int read = in.read(buffer);
							if (read <= 0) break;
							out.write(buffer, 0, read);
						}
						out.close();

                        if (compressedFile.length() == 0) {
                            //noinspection ResultOfMethodCallIgnored
                            compressedFile.delete();
                        } else {
                            //noinspection ResultOfMethodCallIgnored
                            openedFile.delete();
                        }
                    } catch (IOException e) {
						System.err.println("Failed to compress log file '"+compressedFile+"'");
                        e.printStackTrace(System.err);
                    } finally {
						close(out);
						close(in);
					}
                }
            }
        }
    }

    private void logInternalError(@NotNull String problem, @Nullable Throwable error){
		SimpleLogFunction.CONSOLE_LOG_FUNCTION.log("com.darkyen.tproll.advanced.LogFileHandler", TimeProvider.CURRENT_TIME_PROVIDER.timeMillis(), TPLogger.ERROR, null, "INTERNAL ERROR: "+problem);
        if (error != null) {
            System.out.flush();
            error.printStackTrace(System.err);
        }
    }

    private static void close(@Nullable Closeable closeable) {
    	if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				System.err.println("close() failed");
				e.printStackTrace(System.err);
			}
		}
	}
}
