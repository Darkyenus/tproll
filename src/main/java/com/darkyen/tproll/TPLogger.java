package com.darkyen.tproll;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.minlog.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import static darkyenus.tinfoilpigeon.logging.PrettyPrinter.append;

/**
 * Lightweight, GC friendly and thread-safe logger implementation.
 * Ignores markers.
 */
@SuppressWarnings("unused")
public final class TPLogger implements Logger {

    private final String name;

    private static byte logLevel;
    private static boolean trace;
    private static boolean debug;
    private static boolean info;
    private static boolean warn;
    private static boolean error;

    public static String levelName(byte logLevel){
        switch (logLevel) {
            case TRACE: return "TRACE";
            case DEBUG: return "DEBUG";
            case INFO: return "INFO";
            case WARN: return "WARN";
            case ERROR: return "ERROR";
            default: return "UNKNOWN LEVEL "+logLevel;
        }
    }

    public static final byte TRACE = 1;
    public static final byte DEBUG = 2;
    public static final byte INFO = 3;
    public static final byte WARN = 4;
    public static final byte ERROR = 5;

    public static void TRACE() {
        logLevel = TRACE;
        trace = debug = info = warn = error = true;
        Log.TRACE();
    }

    public static void DEBUG() {
        logLevel = DEBUG;
        trace = false;
        debug = info = warn = error = true;
        Log.DEBUG();
    }

    public static void INFO() {
        logLevel = INFO;
        trace = debug = false;
        info = warn = error = true;
        Log.INFO();
    }

    public static void WARN() {
        logLevel = WARN;
        trace = debug = info = false;
        warn = error = true;
        Log.WARN();
    }

    public static void ERROR() {
        logLevel = ERROR;
        trace = debug = info = warn = false;
        error = true;
        Log.ERROR();
    }

    static {
        INFO();//Info is default log level

        //Hook MinLog
        Log.setLogger(new Log.Logger(){
            @Override
            public void log(int level, String category, String message, Throwable ex) {
                logFunction.log(category, System.currentTimeMillis() - START_TIME, (byte)level, message, ex);
            }
        });
    }

    public static byte getLogLevel(){
        return logLevel;
    }

    private static LogFunction logFunction = new LogFunction() {
        @Override
        public synchronized void log(String name, long time, byte level, CharSequence content, Throwable error) {
            System.err.println(name + " " + time + " [" + levelName(level) + "]: " + content);
            if (error != null) error.printStackTrace(System.err);
        }
    };

    public static void setLogFunction(LogFunction logFunction) {
        assert logFunction != null;
        TPLogger.logFunction = logFunction;
    }

    public TPLogger(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return trace;
    }

    @Override
    public void trace(String msg) {
        if (trace) log(TRACE, msg);
    }

    @Override
    public void trace(String format, Object arg) {
        if (trace) log(TRACE, format, arg);
    }

    @Override
    public void trace(String format, Object argA, Object argB) {
        if (trace) log(TRACE, format, argA, argB);
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (trace) log(TRACE, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (trace) log(TRACE, msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return trace;
    }

    @Override
    public void trace(Marker marker, String msg) {
        trace(msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        trace(format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        trace(format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        trace(format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return debug;
    }

    @Override
    public void debug(String msg) {
        if (debug) log(DEBUG, msg);
    }

    @Override
    public void debug(String format, Object arg) {
        if (debug) log(DEBUG, format, arg);
    }

    @Override
    public void debug(String format, Object argA, Object argB) {
        if (debug) log(DEBUG, format, argA, argB);
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (debug) log(DEBUG, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (debug) log(DEBUG, msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return debug;
    }

    @Override
    public void debug(Marker marker, String msg) {
        debug(msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        debug(format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        debug(format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        debug(format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return info;
    }

    @Override
    public void info(String msg) {
        if (info) log(INFO, msg);
    }

    @Override
    public void info(String format, Object arg) {
        if (info) log(INFO, format, arg);
    }

    @Override
    public void info(String format, Object argA, Object argB) {
        if (info) log(INFO, format, argA, argB);
    }

    @Override
    public void info(String format, Object... arguments) {
        if (info) log(INFO, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        if (info) log(INFO, msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return info;
    }

    @Override
    public void info(Marker marker, String msg) {
        info(msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        info(format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        info(format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        info(format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return warn;
    }

    @Override
    public void warn(String msg) {
        if (warn) log(WARN, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        if (warn) log(WARN, format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (warn) log(WARN, format, arguments);
    }

    @Override
    public void warn(String format, Object argA, Object argB) {
        if (warn) log(WARN, format, argA, argB);
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (warn) log(WARN, msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public void warn(Marker marker, String msg) {
        warn(msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        warn(format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        warn(format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        warn(format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return error;
    }

    @Override
    public void error(String msg) {
        if (error) log(ERROR, msg);
    }

    @Override
    public void error(String format, Object arg) {
        if (error) log(ERROR, format, arg);
    }

    @Override
    public void error(String format, Object argA, Object argB) {
        if (error) log(ERROR, format, argA, argB);
    }

    @Override
    public void error(String format, Object... arguments) {
        if (error) log(ERROR, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        if (error) log(ERROR, msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return error;
    }

    @Override
    public void error(Marker marker, String msg) {
        error(msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        error(format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        error(format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        error(format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        error(msg, t);
    }

    private void log(byte level, String msg) {
        synchronized (FORMAT_LOCK) {
            doLog(level, msg);
        }
    }

    private void log(byte level, String format, Object arg) {
        synchronized (FORMAT_LOCK) {
            objects.add(arg);
            doLog(level, format);
        }
    }

    private void log(byte level, String format, Object argA, Object argB) {
        synchronized (FORMAT_LOCK) {
            objects.add(argA);
            objects.add(argB);
            doLog(level, format);
        }
    }

    private void log(byte level, String format, Object... arguments) {
        synchronized (FORMAT_LOCK) {
            objects.addAll(arguments);
            doLog(level, format);
        }
    }

    private static final long START_TIME = System.currentTimeMillis();
    private final Object FORMAT_LOCK = new Object();
    private final Array<Object> objects = new Array<>(true, 8, Object.class);
    private final StringBuilder sb = new StringBuilder(64);

    private void doLog(byte level, String message) {
        final Array<Object> objects = this.objects;
        final StringBuilder sb = this.sb;

        final long sinceStart = (System.currentTimeMillis() - START_TIME);

        if (objects.size == 0) {
            sb.append(message);
            logFunction.log(name, sinceStart, level, sb, null);
        } else {
            boolean escaping = false;
            boolean substituting = false;
            int substitutingIndex = 0;
            Throwable throwable = null;

            for (int i = 0, l = message.length(); i < l; i++) {
                final char c = message.charAt(i);
                if (substituting) {
                    substituting = false;
                    if (c == '}') {
                        if (substitutingIndex != objects.size) {
                            final Object item = objects.items[substitutingIndex];
                            if (item instanceof Throwable) {
                                throwable = (Throwable) item;
                            }
                            append(sb, item);
                            substitutingIndex++;
                        } else {
                            sb.append("{}");
                        }
                        continue;
                    } else {
                        sb.append('{');
                    }
                }

                if (c == '\\') {
                    if (escaping) {
                        sb.append('\\');
                    } else {
                        escaping = true;
                    }
                } else if (c == '{') {
                    if (escaping) {
                        escaping = false;
                        sb.append('{');
                    } else {
                        substituting = true;
                    }
                } else {
                    sb.append(c);
                }
            }
            //There are items that were not appended yet, because they have no {}
            //It could be just one throwable, in that case do not substitute it in
            if(substitutingIndex == objects.size - 1 && objects.items[substitutingIndex] instanceof Throwable){
                throwable = (Throwable) objects.items[substitutingIndex];
            } else if (substitutingIndex < objects.size) {
                //It is not one throwable. It could be more things ended with throwable though
                sb.append(" {");
                do{
                    final Object item = objects.items[substitutingIndex];
                    append:{
                        if (item instanceof Throwable) {
                            throwable = (Throwable) item;
                            if(substitutingIndex == objects.size - 1) {
                                //When throwable is last in list and not in info string, don't print it.
                                //It is guaranteed that it will be printed by trace.
                                break append;
                            }
                        }
                        append(sb, item);
                    }
                    substitutingIndex++;

                    sb.append(", ");
                }while(substitutingIndex < objects.size);
                sb.setLength(sb.length() - 2);
                sb.append('}');
            }
            objects.clear();
            logFunction.log(name, sinceStart, level, sb, throwable);
        }
        sb.setLength(0);
    }

    public interface LogFunction {
        /**
         * Called when logger needs to log a message. Called only when that log level is enabled in the logger.
         * Can be called by any thread, and thus MUST be thread safe.
         *
         * @param name of the logger
         * @param time in ms since start of the app
         * @param level of this message
         * @param content of this message, formatted. Do not keep around!
         * @param error holding the stack trace logging function should handle
         */
        void log(String name, long time, byte level, CharSequence content, Throwable error);
    }

    public static void attachUnhandledExceptionLogger(){
        final Logger logger = LoggerFactory.getLogger("UnhandledException");
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            logger.error("{} has crashed with exception: {}",t, e);
            if(originalHandler != null){
                originalHandler.uncaughtException(t, e);
            }
        });
    }
}

