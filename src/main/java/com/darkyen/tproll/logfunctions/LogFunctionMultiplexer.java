package com.darkyen.tproll.logfunctions;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.util.SimpleMarker;
import org.slf4j.Marker;

import java.util.Iterator;

/**
 * Technically a de-multiplexer, allows usage of markers to divert messages to different/multiple log functions.
 */
public final class LogFunctionMultiplexer implements LogFunction {

    private static final int MAX_TARGETS = Long.BYTES * 8;

    private final LogFunction defaultTarget;
    private LogFunction[] muxTargets = {};

    public LogFunctionMultiplexer(LogFunction defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    public synchronized MuxMarker addMuxTarget(LogFunction function) {
        final LogFunction[] oldTargets = this.muxTargets;
        final int newIndex = oldTargets.length;
        if (newIndex > MAX_TARGETS) throw new IllegalStateException("Too many targets, max is "+MAX_TARGETS);
        final LogFunction[] newTargets = new LogFunction[newIndex+1];
        System.arraycopy(oldTargets, 0, newTargets, 0, newIndex);
        newTargets[newIndex] = function;
        this.muxTargets = newTargets;
        return new MuxMarker(this, newIndex);
    }

    @Override
    public void log(String name, long time, byte level, Marker marker, CharSequence content, Throwable error) {
        if (marker == null) {
            defaultTarget.log(name, time, level, null, content, error);
        } else {
            long remainingTargetMask = findMuxTargets(this, marker);
            final LogFunction[] muxTargets = this.muxTargets;
            int target = 0;
            for (long mask = 1; remainingTargetMask != 0; mask <<= 1, target++) {
                if ((remainingTargetMask & mask) != 0) {
                    remainingTargetMask &= ~mask;
                    muxTargets[target].log(name, time, level, marker, content, error);
                }
            }
        }
    }

    @SuppressWarnings("serialVersionUID")
    public static class MuxMarker extends SimpleMarker {
        private final String name;
        private final LogFunctionMultiplexer multiplexer;
        private final long mask;

        private MuxMarker(LogFunctionMultiplexer multiplexer, int index) {
            super();
            this.multiplexer = multiplexer;
            this.name = "Mux"+index;
            this.mask = 1 << index;
        }

        private MuxMarker(Marker[] references, String name, LogFunctionMultiplexer multiplexer, long mask) {
            super(references);
            this.name = name;
            this.multiplexer = multiplexer;
            this.mask = mask;
        }

        public MuxMarker newCompound(Marker with) {
            final MuxMarker copy = copy();
            copy.add(with);
            return copy;
        }

        @Override
        public String getName() {
            return name;
        }

        public MuxMarker copy() {
            return new MuxMarker(references(), name, multiplexer, mask);
        }
    }

    private static long findMuxTargets(LogFunctionMultiplexer multiplexer, Marker from) {
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