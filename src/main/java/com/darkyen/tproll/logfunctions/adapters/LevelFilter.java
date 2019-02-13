package com.darkyen.tproll.logfunctions.adapters;

import com.darkyen.tproll.LogFunction;
import org.slf4j.Marker;

/**
 * LogFunction adapter, which allows filtering of messages with too low or too high log level
 */
@SuppressWarnings("unused")
public final class LevelFilter extends LogFunction {

    private final LogFunction parent;
    private final byte minLevel, maxLevel;

    /**
     * @param parent function to call if it passes the check
     * @param minLevel of log messages that still gets through
     * @param maxLevel of log messages that still gets through
     */
    public LevelFilter(LogFunction parent, byte minLevel, byte maxLevel) {
        this.parent = parent;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    /**
     * Unbounded at the top
     * @param parent function to call if it passes the check
     * @param minLevel of log messages that still gets through
     */
    public LevelFilter(LogFunction parent, byte minLevel) {
        this.parent = parent;
        this.minLevel = minLevel;
        this.maxLevel = Byte.MAX_VALUE;
    }

    @Override
    public void log(String name, long time, byte level, Marker marker, CharSequence content) {
        if (level >= minLevel && level <= maxLevel) {
            parent.log(name, time, level, marker, content);
        }
    }

    @Override
    public boolean isEnabled(byte level, Marker marker) {
        return level >= minLevel && level <= maxLevel && parent.isEnabled(level, marker);
    }
}
