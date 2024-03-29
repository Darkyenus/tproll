package com.darkyen.tproll;

import com.darkyen.tproll.logfunctions.SimpleLogFunction;
import com.darkyen.tproll.util.LevelChangeListener;
import com.darkyen.tproll.util.TimeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.ArrayList;

import static com.darkyen.tproll.util.PrettyPrinter.patternSubstituteInto;

/**
 * Lightweight, GC friendly and thread-safe (as long as the logging function is thread-safe)
 * logger implementation.
 *
 * Default log level is INFO, default time provider is {@link TimeProvider#CURRENT_TIME_PROVIDER},
 * default level change listener is {@link LevelChangeListener#LOG} and
 * default log function is {@link com.darkyen.tproll.logfunctions.SimpleLogFunction#CONSOLE_LOG_FUNCTION}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class TPLogger implements Logger {

    //region Non-static
    private final @NotNull String name;

    public TPLogger(@NotNull String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }
    //endregion

    public static final byte TRACE = 1;
    public static final byte DEBUG = 2;
    public static final byte INFO = 3;
    public static final byte WARN = 4;
    public static final byte ERROR = 5;
    /** Special level which always gets through, used for logging-related messages. */
    public static final byte LOG = 6;

    private static @NotNull LogFunction logFunction = SimpleLogFunction.EMERGENCY_LOG_FUNCTION;
    static {
        logFunction.start();
    }
    private static @NotNull LevelChangeListener levelChangeListener = LevelChangeListener.LOG;
    private static @NotNull TimeProvider timeProvider = TimeProvider.CURRENT_TIME_PROVIDER;

    private static @Nullable Thread shutdownHook = null;

    private static byte logLevel = INFO;
    private static boolean trace = false;
    private static boolean debug = false;
    private static boolean info = true;
    private static boolean warn = true;
    private static boolean error = true;

    public static @NotNull String levelName(byte logLevel){
        switch (logLevel) {
            case TRACE: return "TRACE";
            case DEBUG: return "DEBUG";
            case INFO: return "INFO";
            case WARN: return "WARN";
            case ERROR: return "ERROR";
            case LOG: return "LOG";
            default: return "UNKNOWN LEVEL "+logLevel;
        }
    }

    public static void TRACE() {
        if (logLevel == TRACE) return;
        logLevel = TRACE;
        trace = debug = info = warn = error = true;
        levelChangeListener.levelChanged(TRACE);
    }

    public static void DEBUG() {
        if (logLevel == DEBUG) return;
        logLevel = DEBUG;
        trace = false;
        debug = info = warn = error = true;
        levelChangeListener.levelChanged(DEBUG);
    }

    public static void INFO() {
        if (logLevel == INFO) return;
        logLevel = INFO;
        trace = debug = false;
        info = warn = error = true;
        levelChangeListener.levelChanged(INFO);
    }

    public static void WARN() {
        if (logLevel == WARN) return;
        logLevel = WARN;
        trace = debug = info = false;
        warn = error = true;
        levelChangeListener.levelChanged(WARN);
    }

    public static void ERROR() {
        if (logLevel == ERROR) return;
        logLevel = ERROR;
        trace = debug = info = warn = false;
        error = true;
        levelChangeListener.levelChanged(ERROR);
    }

    public static byte getLogLevel(){
        return logLevel;
    }

    /**
     * Like {@link #setLogFunction(LogFunction, boolean)} with setupShutdownHook=true.
     */
    public static void setLogFunction(@NotNull LogFunction logFunction) {
        setLogFunction(logFunction, true);
    }

    /**
     * Set the new log function to use. This invokes {@link LogFunction#start()} and {@link LogFunction#stop()}.
     * @param logFunction to log to now
     * @param setupShutdownHook when true, the last logFunction will be stopped on JVM exit
     */
    public static synchronized void setLogFunction(@NotNull LogFunction logFunction, boolean setupShutdownHook) {
        //noinspection ConstantConditions
        if (logFunction == null) throw new NullPointerException("logFunction may not be null");

        final LogFunction oldLogFunction = TPLogger.logFunction;
        if (oldLogFunction == logFunction) {
            return;
        }
        final Thread oldShutdownHook = TPLogger.shutdownHook;
        if (oldShutdownHook != null) {
            TPLogger.shutdownHook = null;
            Runtime.getRuntime().removeShutdownHook(oldShutdownHook);
        }

        TPLogger.logFunction = SimpleLogFunction.EMERGENCY_LOG_FUNCTION;// Temporary, nothing should log now anyway
        oldLogFunction.stop();
        logFunction.start();
        TPLogger.logFunction = logFunction;

        if (setupShutdownHook) {
            Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread(TPLogger::autoShutdown, "TPLoggerShutdownHook"));
        }
    }

    private static synchronized void autoShutdown() {
        if (shutdownHook == Thread.currentThread()) {
            shutdownHook = null;
            final LogFunction shutdownFunction = TPLogger.logFunction;
            TPLogger.logFunction = SimpleLogFunction.EMERGENCY_LOG_FUNCTION;
            shutdownFunction.stop();
        }
    }

    public static @NotNull LogFunction getLogFunction() {
        return logFunction;
    }

    public static void setLevelChangeListener(@NotNull LevelChangeListener levelChangeListener) {
        //noinspection ConstantConditions
        if (levelChangeListener == null) throw new NullPointerException("levelChangeListener may not be null");
        TPLogger.levelChangeListener = levelChangeListener;
    }

    public static @NotNull LevelChangeListener getLevelChangeListener() {
        return levelChangeListener;
    }

    public static void setTimeProvider(@NotNull TimeProvider timeProvider) {
        //noinspection ConstantConditions
        if (timeProvider == null) throw new NullPointerException("timeProvider may not be null");
        TPLogger.timeProvider = timeProvider;
    }

    public static @NotNull TimeProvider getTimeProvider() {
        return timeProvider;
    }

    //region isEnabled
    @Override
    public boolean isTraceEnabled() {
        return trace && logFunction.isEnabled(TRACE, null);
    }

    @Override
    public boolean isTraceEnabled(@Nullable Marker marker) {
        return trace && logFunction.isEnabled(TRACE, marker);
    }

    @Override
    public boolean isDebugEnabled() {
        return debug && logFunction.isEnabled(DEBUG, null);
    }

    @Override
    public boolean isDebugEnabled(@Nullable Marker marker) {
        return debug && logFunction.isEnabled(DEBUG, marker);
    }

    @Override
    public boolean isInfoEnabled() {
        return info && logFunction.isEnabled(INFO, null);
    }

    @Override
    public boolean isInfoEnabled(@Nullable Marker marker) {
        return info && logFunction.isEnabled(INFO, marker);
    }

    @Override
    public boolean isWarnEnabled() {
        return warn && logFunction.isEnabled(WARN, null);
    }

    @Override
    public boolean isWarnEnabled(@Nullable Marker marker) {
        return warn && logFunction.isEnabled(WARN, marker);
    }

    @Override
    public boolean isErrorEnabled() {
        return error && logFunction.isEnabled(ERROR, null);
    }

    @Override
    public boolean isErrorEnabled(@Nullable Marker marker) {
        return error && logFunction.isEnabled(ERROR, marker);
    }
    //endregion

    //region Trace
    @Override
    public void trace(@NotNull String msg) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, null, msg);
    }

    @Override
    public void trace(@NotNull String format, @Nullable Object arg) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, null, format, arg);
    }

    @Override
    public void trace(@NotNull String format, @Nullable Object argA, @Nullable Object argB) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, null, format, argA, argB);
    }

    @Override
    public void trace(@NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, null, format, arguments);
    }

    @Override
    public void trace(@NotNull String msg, @Nullable Throwable t) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, null, msg, t);
    }

    @Override
    public void trace(@Nullable Marker marker, @NotNull String msg) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, marker, msg);
    }

    @Override
    public void trace(@Nullable Marker marker, @NotNull String format, @Nullable Object arg) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, marker, format, arg);
    }

    @Override
    public void trace(@Nullable Marker marker, @NotNull String format, @Nullable Object arg1, @Nullable Object arg2) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, marker, format, arg1, arg2);
    }

    @Override
    public void trace(@Nullable Marker marker, @NotNull String format, Object @NotNull ... argArray) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, marker, format, argArray);
    }

    @Override
    public void trace(@Nullable Marker marker, @NotNull String msg, @Nullable Throwable t) {
        if (trace) _log(name, timeProvider.timeMillis(), TRACE, marker, msg, t);
    }
    //endregion

    //region Debug
    @Override
    public void debug(@NotNull String msg) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, null, msg);
    }

    @Override
    public void debug(@NotNull String format, @Nullable Object arg) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, null, format, arg);
    }

    @Override
    public void debug(@NotNull String format, @Nullable Object argA, @Nullable Object argB) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, null, format, argA, argB);
    }

    @Override
    public void debug(@NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, null, format, arguments);
    }

    @Override
    public void debug(@NotNull String msg, @Nullable Throwable t) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, null, msg, t);
    }


    @Override
    public void debug(@Nullable Marker marker, @NotNull String msg) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, marker, msg);
    }

    @Override
    public void debug(@Nullable Marker marker, @NotNull String format, @Nullable Object arg) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, marker, format, arg);
    }

    @Override
    public void debug(@Nullable Marker marker, @NotNull String format, @Nullable Object arg1, @Nullable Object arg2) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, marker, format, arg1, arg2);
    }

    @Override
    public void debug(@Nullable Marker marker, @NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, marker, format, arguments);
    }

    @Override
    public void debug(@Nullable Marker marker, @NotNull String msg, @Nullable Throwable t) {
        if (debug) _log(name, timeProvider.timeMillis(), DEBUG, marker, msg, t);
    }
    //endregion

    //region Info
    @Override
    public void info(@NotNull String msg) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, null, msg);
    }

    @Override
    public void info(@NotNull String format, @Nullable Object arg) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, null, format, arg);
    }

    @Override
    public void info(@NotNull String format, @Nullable Object argA, @Nullable Object argB) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, null, format, argA, argB);
    }

    @Override
    public void info(@NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, null, format, arguments);
    }

    @Override
    public void info(@NotNull String msg, @Nullable Throwable t) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, null, msg, t);
    }

    @Override
    public void info(@Nullable Marker marker, @NotNull String msg) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, marker, msg);
    }

    @Override
    public void info(@Nullable Marker marker, @NotNull String format, @Nullable Object arg) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, marker, format, arg);
    }

    @Override
    public void info(@Nullable Marker marker, @NotNull String format, @Nullable Object arg1, @Nullable Object arg2) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, marker, format, arg1, arg2);
    }

    @Override
    public void info(@Nullable Marker marker, @NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, marker, format, arguments);
    }

    @Override
    public void info(@Nullable Marker marker, @NotNull String msg, @Nullable Throwable t) {
        if (info) _log(name, timeProvider.timeMillis(), INFO, marker, msg, t);
    }
    //endregion

    //region Warn
    @Override
    public void warn(@NotNull String msg) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, null, msg);
    }

    @Override
    public void warn(@NotNull String format, @Nullable Object arg) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, null, format, arg);
    }

    @Override
    public void warn(@NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, null, format, arguments);
    }

    @Override
    public void warn(@NotNull String format, @Nullable Object argA, @Nullable Object argB) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, null, format, argA, argB);
    }

    @Override
    public void warn(@NotNull String msg, @Nullable Throwable t) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, null, msg, t);
    }

    @Override
    public void warn(@Nullable Marker marker, @NotNull String msg) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, marker, msg);
    }

    @Override
    public void warn(@Nullable Marker marker, @NotNull String format, @Nullable Object arg) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, marker, format, arg);
    }

    @Override
    public void warn(@Nullable Marker marker, @NotNull String format, @Nullable Object arg1, @Nullable Object arg2) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, marker, format, arg1, arg2);
    }

    @Override
    public void warn(@Nullable Marker marker, @NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, marker, format, arguments);
    }

    @Override
    public void warn(@Nullable Marker marker, @NotNull String msg, @Nullable Throwable t) {
        if (warn) _log(name, timeProvider.timeMillis(), WARN, marker, msg, t);
    }
    //endregion

    //region Error
    @Override
    public void error(@NotNull String msg) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, null, msg);
    }

    @Override
    public void error(@NotNull String format, @Nullable Object arg) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, null, format, arg);
    }

    @Override
    public void error(@NotNull String format, @Nullable Object argA, @Nullable Object argB) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, null, format, argA, argB);
    }

    @Override
    public void error(@NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, null, format, arguments);
    }

    @Override
    public void error(@NotNull String msg, @Nullable Throwable t) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, null, msg, t);
    }

    @Override
    public void error(@Nullable Marker marker, @NotNull String msg) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, marker, msg);
    }

    @Override
    public void error(@Nullable Marker marker, @NotNull String format, @Nullable Object arg) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, marker, format, arg);
    }

    @Override
    public void error(@Nullable Marker marker, @NotNull String format, @Nullable Object arg1, @Nullable Object arg2) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, marker, format, arg1, arg2);
    }

    @Override
    public void error(@Nullable Marker marker, @NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, marker, format, arguments);
    }

    @Override
    public void error(@Nullable Marker marker, @NotNull String msg, @Nullable Throwable t) {
        if (error) _log(name, timeProvider.timeMillis(), ERROR, marker, msg, t);
    }
    //endregion

    //region Log
    public void log(byte level, @Nullable Marker marker, @NotNull String msg) {
        if (level < TPLogger.logLevel) return;
        _log(name, timeProvider.timeMillis(), level, marker, msg);
    }

    public void log(byte level, @Nullable Marker marker, @NotNull String format, @Nullable Object arg) {
        if (level < TPLogger.logLevel) return;
        _log(name, timeProvider.timeMillis(), level, marker, format, arg);
    }

    public void log(byte level, @Nullable Marker marker, @NotNull String format, @Nullable Object argA, @Nullable Object argB) {
        if (level < TPLogger.logLevel) return;
        _log(name, timeProvider.timeMillis(), level, marker, format, argA, argB);
    }

    public void log(byte level, @Nullable Marker marker, @NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (level < TPLogger.logLevel) return;
        _log(name, timeProvider.timeMillis(), level, marker, format, arguments);
    }

    public void log(byte level, @Nullable Marker marker, @NotNull String msg, @Nullable Throwable t) {
        if (level < TPLogger.logLevel) return;
        _log(name, timeProvider.timeMillis(), level, marker, msg, t);
    }
    //endregion

    //region Custom

    /**
     * Custom log functions for easier integration with other log systems.
     * @param name Logger name
     * @param time time, as provided by {@link TimeProvider}
     */
    public void logCustom(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull String msg) {
        if (level < TPLogger.logLevel) return;
        _log(name, time, level, marker, msg);
    }

    public void logCustom(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull String format, @Nullable Object arg) {
        if (level < TPLogger.logLevel) return;
        _log(name, time, level, marker, format, arg);
    }

    public void logCustom(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull String format, @Nullable Object argA, @Nullable Object argB) {
        if (level < TPLogger.logLevel) return;
        _log(name, time, level, marker, format, argA, argB);
    }

    public void logCustom(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull String format, @Nullable Object @NotNull ... arguments) {
        if (level < TPLogger.logLevel) return;
        _log(name, time, level, marker, format, arguments);
    }

    public void logCustom(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull String msg, @Nullable Throwable t) {
        if (level < TPLogger.logLevel) return;
        _log(name, time, level, marker, msg, t);
    }
    //endregion
    
    //------------------------------------- INTERNAL ----------------------------------------------------

    private void _log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull String msg) {
        if(!logFunction.isEnabled(level, marker)) return;
        final ThreadLogState state = ThreadLogState.INSTANCE.get();
        final ArrayList<@Nullable Object> arguments = state.arguments;
        state.doLog(name, time, level, marker, msg);
    }

    private void _log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull String format, @Nullable Object arg) {
        if(!logFunction.isEnabled(level, marker)) return;
        final ThreadLogState state = ThreadLogState.INSTANCE.get();
        final ArrayList<@Nullable Object> arguments = state.arguments;
        arguments.add(arg);
        state.doLog(name, time, level, marker, format);
    }

    private void _log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull String format, @Nullable Object argA, @Nullable Object argB) {
        if(!logFunction.isEnabled(level, marker)) return;
        final ThreadLogState state = ThreadLogState.INSTANCE.get();
        final ArrayList<@Nullable Object> arguments = state.arguments;
        arguments.add(argA);
        arguments.add(argB);
        state.doLog(name, time, level, marker, format);
    }

    private void _log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull String format, @Nullable Object @NotNull ... arguments) {
        if(!logFunction.isEnabled(level, marker)) return;
        final ThreadLogState state = ThreadLogState.INSTANCE.get();
        final ArrayList<@Nullable Object> args = state.arguments;
        args.ensureCapacity(arguments.length);
        //noinspection ManualArrayToCollectionCopy
        for (Object argument : arguments) {
            //noinspection UseBulkOperation
            args.add(argument);
        }
        state.doLog(name, time, level, marker, format);
    }

    /** Will call {@link Thread#setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}
     * with a function that logs these exceptions. If there already is a handler, it is called after the exception is logged. */
    public static void attachUnhandledExceptionLogger(){
        final Logger logger = LoggerFactory.getLogger("UnhandledException");
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.error("{} has crashed with exception: {}", t, e);
                if (originalHandler != null) {
                    originalHandler.uncaughtException(t, e);
                }
            }
        });
    }

    private static final class ThreadLogState {
        final @NotNull ArrayList<@Nullable Object> arguments = new ArrayList<>();
        final @NotNull StringBuilder sb = new StringBuilder(64);

        void doLog(final @NotNull String name, final long time, final byte level, final @Nullable Marker marker, final @NotNull String message) {
            final StringBuilder sb = this.sb;
            final ArrayList<@Nullable Object> arguments = this.arguments;
            try {
                patternSubstituteInto(sb, message, arguments);
                if (!TPLogger.logFunction.log(name, time, level, marker, sb)) {
                    SimpleLogFunction.EMERGENCY_LOG_FUNCTION.log(name, time, level, marker, sb);
                }
            } finally {
                sb.setLength(0);
                arguments.clear();
            }
        }

        static final @NotNull ThreadLocal<@NotNull ThreadLogState> INSTANCE = ThreadLocal.withInitial(ThreadLogState::new);
    }
}

