package com.darkyen.tproll.logfunctions.adapters;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.logfunctions.AbstractAdapterLogFunction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Marker;

/**
 * LogFunction adapter, which allows filtering of messages with too low or too high log level
 */
@SuppressWarnings("unused")
public final class LevelFilter extends AbstractAdapterLogFunction {

    private final byte minLevel, maxLevel;

    /**
     * @param parent function to call if it passes the check
     * @param minLevel of log messages that still gets through
     * @param maxLevel of log messages that still gets through
     */
    public LevelFilter(@NotNull LogFunction parent, byte minLevel, byte maxLevel) {
        super(parent);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    /**
     * Unbounded at the top
     * @param parent function to call if it passes the check
     * @param minLevel of log messages that still gets through
     */
    public LevelFilter(@NotNull LogFunction parent, byte minLevel) {
        this(parent, minLevel, Byte.MAX_VALUE);
    }

    @Override
    public boolean log(@NotNull String name, long time, byte level, Marker marker, @NotNull CharSequence content) {
        if (level >= minLevel && level <= maxLevel) {
            return parent.log(name, time, level, marker, content);
        }
        return true;
    }

    @Override
    public boolean isEnabled(byte level, Marker marker) {
        return level >= minLevel && level <= maxLevel && parent.isEnabled(level, marker);
    }
}
