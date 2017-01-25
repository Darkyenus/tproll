package com.darkyen.tproll;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 *
 */
public class FileLogFunction implements TPLogger.LogFunction {

    private static final String EXTENSION = ".log";

    private PrintWriter fileWriter;
    private final StringBuilder sb;

    private final byte fileLevel;
    private final byte consoleLevel;

    private String consolePrefix = "";

    private final Object LOCK = new Object();

    public FileLogFunction(File logFolder, LogFileCreationStrategy fileCreationStrategy, byte fileLevel, byte consoleLevel) {
        PrintWriter fileWriter;
        try {
            final File logFile = fileCreationStrategy.getLogFile(logFolder);

            //Verify that the file is valid
            if(logFile == null) {
                throw new Exception("File creation strategy returned null");
            } else if(logFile.isDirectory()) {
                throw new Exception("Returned log file at '" + logFile.getAbsolutePath() + "' is a directory");
            } else {
                final File parentFile = logFile.getParentFile();
                if(parentFile != null){
                    if(parentFile.exists()){
                        if(!parentFile.isDirectory()){
                            throw new Exception("Parent file of '"+logFile.getAbsolutePath()+"' exists and is not a directory");
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
                    throw new Exception("Returned file can't be written to");
                }
            }else{
                if(logFile.exists()){
                    throw new Exception("Returned file at '"+logFile.getAbsolutePath()+"' is not a file but exists");
                }
            }

            fileWriter = new PrintWriter(new FileWriter(logFile, fileCreationStrategy.shouldAppend()), true);
            fileWriter.println("Log file opened");
        } catch (Exception e) {
            logInternalError("Log file creation failed, being System.out/err only.",e);
            fileWriter = null;
        }

        this.fileWriter = fileWriter;
        this.sb = new StringBuilder();
        this.fileLevel = fileLevel;
        this.consoleLevel = consoleLevel;
    }

    /**
     * Set text to be added in front of all console text.
     * Useful for distinguishing the text among other logs.
     * Ending the prefix with a space is recommended for aesthetic reasons.
     *
     * The prefix is added only before explicit lines (stack traces sadly do not have it)
     */
    public void setConsolePrefix(String consolePrefix) {
        synchronized (LOCK) {
            if(consolePrefix == null)consolePrefix = "";
            this.consolePrefix = consolePrefix;
        }
    }

    @Override
    public void log(String name, long time, byte level, CharSequence content, Throwable error) {
        synchronized (LOCK) {
            if(level >= fileLevel && fileWriter != null){
                logFile(name, time, level, content, error);
            }
            if(level >= consoleLevel){
                logConsole(name, time, level, content, error);
            }
        }
    }

    private void logConsole(String name, @SuppressWarnings("UnusedParameters") long time, byte level, CharSequence content, Throwable error) {
        final StringBuilder sb = this.sb;
        sb.append(consolePrefix).append('[').append(TPLogger.levelName(level)).append("] ").append(name).append(": ").append(content);

        final PrintStream out = level < TPLogger.ERROR ? System.out : System.err;

        out.println(sb);
        if(error != null)error.printStackTrace(out);

        sb.setLength(0);
    }

    private void logFile(String name, long time, byte level, CharSequence content, Throwable error) {
        final StringBuilder sb = this.sb;
        long seconds = time / 1000;
        long minutes = seconds / 60;
        final long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;
        sb.append('[').append(TPLogger.levelName(level)).append("] ")
                .append(hours).append(':');
        if(minutes < 10)sb.append('0');
        sb.append(minutes).append(':');
        if(seconds < 10)sb.append('0');
        sb.append(seconds).append(' ').append(name).append(": ").append(content).append('\n');

        final PrintWriter fileWriter = this.fileWriter;
        fileWriter.print(sb);
        if(error != null){
            error.printStackTrace(fileWriter);
        }
        if(fileWriter.checkError()){//Flushes
            logInternalError("FileWriter has encountered an unknown error", null);
        }
        sb.setLength(0);
    }

    private void logInternalError(String problem, Throwable error){
        System.err.println("darkyenus.tinfoilpigeon.logging.FileLogFunction: INTERNAL ERROR: "+problem);
        if(error != null)error.printStackTrace(System.err);
    }

    public void dispose(){
        synchronized (LOCK) {
            if(fileWriter != null){
                fileWriter.println("Log file closed");
                if(fileWriter.checkError()){
                    logInternalError("FileWriter has encountered an unknown error (in dispose())", null);
                }
                fileWriter.close();
                fileWriter = null;
            }
        }
    }

    public boolean registerShutdownDisposeHook(){
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    dispose();
                }
            });
            return true;
        } catch (Throwable ex) {
            logInternalError("Error during registering shutdown hook", ex);
            return false;
        }
    }

    public static FileLogFunction enable(File logFolder){
        return enable(logFolder, TPLogger.WARN, TPLogger.TRACE);
    }

    public static FileLogFunction enable(File logFolder, byte fileLogLevel, byte consoleLogLevel){
        final FileLogFunction logFunction = new FileLogFunction(logFolder, DATE_TIME_CREATION_STRATEGY, fileLogLevel, consoleLogLevel);
        TPLogger.setLogFunction(logFunction);
        TPLogger.attachUnhandledExceptionLogger();
        logFunction.registerShutdownDisposeHook();
        return logFunction;
    }

    public interface LogFileCreationStrategy {
        File getLogFile(File logFolder) throws Exception;
        default boolean shouldAppend(){
            return false;
        }
    }

    public static final LogFileCreationStrategy DATE_TIME_CREATION_STRATEGY = new LogFileCreationStrategy() {

        private final DateTimeFormatter LOG_FILE_NAME_FORMATTER = new DateTimeFormatterBuilder()
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

        @Override
        public File getLogFile(File logFolder) throws Exception {
            final StringBuilder sb = new StringBuilder();
            final LocalDateTime now = LocalDateTime.now();

            LOG_FILE_NAME_FORMATTER.printTo(sb, now);
            final int dateLength = sb.length();

            sb.append(EXTENSION);

            File logFile = new File(logFolder, sb.toString());
            if(logFile.exists()){
                //Need to try appendages
                tryAppendages: {
                    for (int i = 2; i < 1000; i++) {
                        sb.setLength(dateLength);
                        sb.append('.').append(i).append(EXTENSION);
                        logFile = new File(logFolder, sb.toString());
                        if(!logFile.exists())break tryAppendages;
                    }
                    //Failed to find non-existing file
                    throw new Exception("Failed to create log file, all variants exist.");
                }
            }

            return logFile;
        }
    };
}
