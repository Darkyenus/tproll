package com.darkyen.tproll.integration;

import com.darkyen.tproll.util.LevelChangeListener;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.util.SimpleMarker;
import com.esotericsoftware.minlog.Log;
import org.jetbrains.annotations.NotNull;

/**
 * Call {@link #enable()} if you want to route MinLog logs through tproll.
 * You must have MinLog in classpath to use this class.
 */
@SuppressWarnings("unused")
public class MinLogIntegration {

    private MinLogIntegration() {
    }

    public static final @NotNull SimpleMarker MIN_LOG_MARKER = new SimpleMarker() {

        private static final long serialVersionUID = 1L;

        @Override
        public String getName() {
            return "MinLog";
        }
    };
    public static final @NotNull TPLogger LOGGER = new TPLogger("MinLog");

    /** Calls {@link Log#setLogger} with a logger which uses logger of {@link TPLogger} for logging.
     * Also sets */
    public static void enable() {
        Log.set(TPLogger.getLogLevel());
        final LevelChangeListener oldChangeListener = TPLogger.getLevelChangeListener();
        TPLogger.setLevelChangeListener(new LevelChangeListener() {
            @Override
            public void levelChanged(byte to) {
                oldChangeListener.levelChanged(to);
                Log.set(to);
            }
        });
        Log.setLogger(new Log.Logger(){
            @Override
            public void log(int level, String category, String message, Throwable ex) {
                final long time = TPLogger.getTimeProvider().timeMillis();
                if (ex == null) {
                    LOGGER.logCustom(category, time, (byte)level, MIN_LOG_MARKER, message);
                } else {
                    LOGGER.logCustom(category, time, (byte)level, MIN_LOG_MARKER, message, ex);
                }
            }
        });
    }
}
