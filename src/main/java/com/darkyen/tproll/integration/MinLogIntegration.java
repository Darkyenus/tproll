package com.darkyen.tproll.integration;

import com.darkyen.tproll.util.LevelChangeListener;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.util.SimpleMarker;
import com.esotericsoftware.minlog.Log;

/**
 * Call {@link #enable()} if you want to route MinLog logs through tproll.
 * You must have MinLog in classpath to use this class.
 *
 * Instantiating this class
 */
@SuppressWarnings("WeakerAccess")
public class MinLogIntegration {

    public static final SimpleMarker MIN_LOG_MARKER = new SimpleMarker() {
        @Override
        public String getName() {
            return "MinLog";
        }
    };

    /** Calls {@link Log#setLogger} with a logger which uses logger of {@link TPLogger} for logging.
     * Also sets */
    public static void enable() {
        Log.set(TPLogger.getLogLevel());
        final LevelChangeListener oldChangeListener = TPLogger.getLevelChangeListener();
        TPLogger.setLevelChangeListener(to -> {
            oldChangeListener.levelChanged(to);
            Log.set(to);
        });
        Log.setLogger(new Log.Logger(){
            @Override
            public void log(int level, String category, String message, Throwable ex) {
                TPLogger.getLogFunction().log(category, TPLogger.getTimeProvider().timeMillis(), (byte)level, MIN_LOG_MARKER, message, ex);
            }
        });
    }
}
