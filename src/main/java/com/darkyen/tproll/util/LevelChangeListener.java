package com.darkyen.tproll.util;

import com.darkyen.tproll.TPLogger;

public interface LevelChangeListener {

    /**
     * Called when log level is changed. Does not have to be implemented,
     * but MUST be thread safe, as it will be called from the thread which changed the log level.
     * This operation is not logged by default.
     *
     * @param to one of {@link TPLogger#TRACE}, {@link TPLogger#DEBUG}, {@link TPLogger#INFO}, {@link TPLogger#WARN} and {@link TPLogger#ERROR} */
    void levelChanged(byte to);

    LevelChangeListener NO_OP = new LevelChangeListener() {
        @Override
        public void levelChanged(byte to) {}
    };

    LevelChangeListener LOG = new LevelChangeListener() {
        final TPLogger LOG = new TPLogger("Log Level");

        @Override
        public void levelChanged(byte to) {
            LOG.log(TPLogger.LOG, null, "Changed to {}", TPLogger.levelName(to));
        }
    };
}
