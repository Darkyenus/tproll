package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.LogFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Marker;

/**
 * LogFunction that delegates everything to parent LogFunction.
 * To be used as a building block for custom LogFunctions.
 */
public abstract class AbstractAdapterLogFunction extends LogFunction {

    protected final @NotNull LogFunction parent;

    protected AbstractAdapterLogFunction(@NotNull LogFunction parent) {
        this.parent = parent;
    }

    @Override
    public boolean log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
        return parent.log(name, time, level, marker, content);
    }

    @Override
    public boolean isEnabled(byte level, @Nullable Marker marker) {
        return parent.isEnabled(level, marker);
    }

    @Override
    public void start() {
        parent.start();
    }

    @Override
    public void stop() {
        parent.stop();
    }
}
