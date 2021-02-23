package com.darkyen.tproll.util;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Marker;

import java.util.Iterator;

/**
 * Markers implementing this interface will get rendered by the log function
 */
public interface RenderableMarker extends Marker {

    /**
     * @return the label to be used for rendering with LogFunction
     */
    default @NotNull String getLabel() {
        return getName();
    }

    /**
     * Convenience method to append all {@link RenderableMarker}s into the {@code sb}.
     * @param ansiColor whether ANSI colors should be used
     * @param firstInList if true, a single space will be added
     */
    static void appendMarker(@NotNull StringBuilder sb, boolean ansiColor, @NotNull Marker marker, boolean firstInList) {
        boolean rendered = false;

        if (marker instanceof RenderableMarker) {
            if (ansiColor) sb.append(AnsiColor.BLACK);
            if (!firstInList) sb.append(' ');
            sb.append("| ");
            if (ansiColor) sb.append(AnsiColor.YELLOW);
            sb.append(((RenderableMarker)marker).getLabel());
            rendered = true;
        }

        if (marker.hasReferences()) {
            for (Iterator<Marker> it = marker.iterator(); it.hasNext(); ) {
                appendMarker(sb, ansiColor, it.next(), firstInList && !rendered);
            }
        }
    }
}

