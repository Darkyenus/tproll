package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.util.TimeProvider;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class LogFileHandler implements ILogFileHandler {

    private static final DateTimeFormatter FILE_ACTION_TIME_FORMATTER = new DateTimeFormatterBuilder()
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
            .appendValue(ChronoField.SECOND_OF_MINUTE,2)
            .appendLiteral(' ')
            .appendZoneId()
            .appendLiteral(' ')
            .appendOffsetId()
            .toFormatter();

    private final TPLogger LOG = new TPLogger("LogFileHandler");

    private final File logFolder;
    private final LogFileCreationStrategy fileCreationStrategy;
    private final boolean compressOnExit;

    private File openedFile = null;
    private PrintWriter fileWriter = null;

    public LogFileHandler(File logFolder, LogFileCreationStrategy fileCreationStrategy, boolean compressOnExit) {
        this.logFolder = logFolder;
        this.fileCreationStrategy = fileCreationStrategy;
        this.compressOnExit = compressOnExit;
    }

    @Override
    public void initialize() {
        PrintWriter fileWriter;
        try {
            final File logFile = fileCreationStrategy.getLogFile(logFolder);

            //Verify that the file is valid
            if(logFile == null) {
                throw new NullPointerException("File creation strategy returned null");
            } else if(logFile.isDirectory()) {
                throw new FileNotFoundException("Returned log file at '" + logFile.getAbsolutePath() + "' is a directory");
            } else {
                final File parentFile = logFile.getParentFile();
                if(parentFile != null){
                    if(parentFile.exists()){
                        if(!parentFile.isDirectory()){
                            throw new FileAlreadyExistsException("Parent file of '"+logFile.getAbsolutePath()+"' exists and is not a directory");
                        }
                    }else{
                        if(!parentFile.mkdirs()){
                            throw new Exception("Failed to create parent directories for '"+logFile.getAbsolutePath()+"'");
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

            fileCreationStrategy.performCleanup(logFolder, logFile, LOG);
        } catch (Exception e) {
            logInternalError("Log file creation failed, being System.err only.", e);
        }
    }

    @Override
    public void log(CharSequence message) {
        final PrintWriter fileWriter = this.fileWriter;
        if (fileWriter != null) {
            fileWriter.append(message);
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
            FILE_ACTION_TIME_FORMATTER.formatTo(TPLogger.getTimeProvider().time(), fileWriter);
            fileWriter.append('\n');
            fileWriter.flush();

            if(fileWriter.checkError()){
                logInternalError("FileWriter has encountered an unknown error (in dispose())", null);
            }
            fileWriter.close();


            if (compressOnExit) {
                final File compressedFile = new File(openedFile.getParentFile(), openedFile.getName()+".gzip");
                if (!compressedFile.exists()) {
                    try(GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(compressedFile))) {
                        try (FileInputStream in = new FileInputStream(openedFile)) {
                            final byte[] buffer = new byte[(int)Math.min(4096, openedFile.length())];
                            while (true) {
                                final int read = in.read(buffer);
                                if (read <= 0) break;
                                out.write(buffer, 0, read);
                            }
                            out.close();
                        }

                        if (compressedFile.length() == 0) {
                            //noinspection ResultOfMethodCallIgnored
                            compressedFile.delete();
                        } else {
                            //noinspection ResultOfMethodCallIgnored
                            openedFile.delete();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void logInternalError(String problem, Throwable error){
        LogFunction.SIMPLE_LOG_FUNCTION.log("com.darkyen.tproll.advanced.LogFileHandler", TimeProvider.CURRENT_TIME_PROVIDER.timeMillis(), TPLogger.ERROR, null, "INTERNAL ERROR: "+problem, error);
    }
}
