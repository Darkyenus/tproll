package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.util.SimpleMarker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Marker;

import java.util.Iterator;

/**
 * Technically a de-multiplexer, allows usage of markers to divert messages to different/multiple log functions.
 */
public final class LogFunctionMultiplexer extends LogFunction {

    private static final int MAX_TARGETS = 8 * 8; //Long.BYTES * 8;

    private @NotNull LogFunction[] muxTargets = {};
    private long optOutMask = 0;

    public LogFunctionMultiplexer() {
    }

    /**
     * Convenience constructor, equivalent (but slightly faster) to using the default constructor and then
     * calling {@link #addMuxTarget(LogFunction, boolean)} for each target supplied and optOut parameter "true".
     * Note that since this does not return any {@link MuxMarker}s, it is not actually possible to "opt-out" from these
     * loggers.
     *
     * @param defaultOptOutTargets Initial mux targets */
    public LogFunctionMultiplexer(@NotNull LogFunction...defaultOptOutTargets) {
        this.muxTargets = defaultOptOutTargets;
        this.optOutMask = (1L << defaultOptOutTargets.length) - 1L;
    }

    /** Add multiplexing target. May be called only before logging starts.
     * If optOut is false, only messages with returned marker are logged.
     * If optOut is true, all messages, except those with returned marker are logged.
     * @param function to which this branch should log */
    public synchronized @NotNull MuxMarker addMuxTarget(@NotNull LogFunction function, boolean optOut) {
        final LogFunction[] oldTargets = this.muxTargets;
        final int newIndex = oldTargets.length;
        if (newIndex > MAX_TARGETS) throw new IllegalStateException("Too many targets, max is "+MAX_TARGETS);
        final LogFunction[] newTargets = new LogFunction[newIndex+1];
        System.arraycopy(oldTargets, 0, newTargets, 0, newIndex);
        newTargets[newIndex] = function;
        this.muxTargets = newTargets;

        if (optOut) {
            optOutMask |= 1L << newIndex;
        }
        return new MuxMarker(this, newIndex);
    }

    /**
     * @return true if at least one target was successful (false if there are no targets)
     */
    @Override
    public boolean log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
        long remainingTargetMask = findMuxTargets(this, marker);
        remainingTargetMask ^= optOutMask;
        final LogFunction[] muxTargets = this.muxTargets;
        boolean success = false;
        int target = 0;
        for (long mask = 1; remainingTargetMask != 0; mask <<= 1, target++) {
            if ((remainingTargetMask & mask) != 0) {
                remainingTargetMask &= ~mask;
                if (muxTargets[target].log(name, time, level, marker, content)) {
                    success = true;
                }
            }
        }
        return success;
    }

    @Override
    public boolean isEnabled(byte level, @Nullable Marker marker) {
        long remainingTargetMask = findMuxTargets(this, marker);
        remainingTargetMask ^= optOutMask;
        final LogFunction[] muxTargets = this.muxTargets;
        int target = 0;
        for (long mask = 1; remainingTargetMask != 0; mask <<= 1, target++) {
            if ((remainingTargetMask & mask) != 0) {
                remainingTargetMask &= ~mask;
                if (muxTargets[target].isEnabled(level, marker)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start() {
        for (LogFunction target : muxTargets) {
            target.start();
        }
    }

    @Override
    public void stop() {
        for (LogFunction target : muxTargets) {
            target.stop();
        }
    }

    @SuppressWarnings("serialVersionUID")
    public static class MuxMarker extends SimpleMarker {

        private static final long serialVersionUID = 1L;

        private final @NotNull String name;
        private final @NotNull LogFunctionMultiplexer multiplexer;
        private final long mask;

        private MuxMarker(@NotNull LogFunctionMultiplexer multiplexer, int index) {
            super();
            this.multiplexer = multiplexer;
            this.name = "Mux"+index;
            this.mask = 1L << index;
        }

        private MuxMarker(@NotNull Marker[] references, @NotNull String name, @NotNull LogFunctionMultiplexer multiplexer, long mask) {
            super(references);
            this.name = name;
            this.multiplexer = multiplexer;
            this.mask = mask;
        }

        public @NotNull MuxMarker newCompound(@NotNull Marker with) {
            final MuxMarker copy = copy();
            copy.add(with);
            return copy;
        }

        @Override
        public @NotNull String getName() {
            return name;
        }

        public @NotNull MuxMarker copy() {
            return new MuxMarker(references(), name, multiplexer, mask);
        }
    }

    private static long findMuxTargets(@NotNull LogFunctionMultiplexer multiplexer, @Nullable Marker from) {
        if (from == null) return 0;
        long result = 0;
        if (from instanceof MuxMarker) {
            final MuxMarker marker = (MuxMarker) from;
            if (marker.multiplexer == multiplexer) {
                result = marker.mask;
            }
        }
        try {
            if (from instanceof SimpleMarker) {
                final Marker[] references = ((SimpleMarker) from).references();
                for (Marker reference : references) {
                    result |= findMuxTargets(multiplexer, reference);
                }
            } else if (from.hasReferences()) {
                final Iterator<Marker> iterator = from.iterator();
                while (iterator.hasNext()) {
                    result |= findMuxTargets(multiplexer, iterator.next());
                }
            }
        } catch (StackOverflowError ex) {
            throw new IllegalArgumentException("Marker "+from.getName()+" (most likely) contains cycles", ex);
        }
        return result;
    }

}
