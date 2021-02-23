package com.darkyen.tproll.util;

import org.slf4j.Marker;

import java.util.Iterator;

import static com.darkyen.tproll.util.TerminalColor.black;
import static com.darkyen.tproll.util.TerminalColor.yellow;

/**
 * Every Marker implementing this interface will get rendered by the log function
 */
public interface RenderableMarker extends Marker {

    /**
     * @return the label to be used for rendering with LogFunction
     */
    default String getLabel() {
        return getName();
    }


    static void appendMarker(StringBuilder sb, Marker marker, boolean firstInList, boolean useColors) {
        boolean rendered = false;

        if (marker instanceof RenderableMarker) {
            if (useColors) black(sb);
            if (firstInList) sb.append(' ');
            sb.append("| ");
            if (useColors) yellow(sb);
            sb.append(((RenderableMarker)marker).getLabel());
            rendered = true;
        }

        if (marker.hasReferences()) {
            for (Iterator<Marker> it = marker.iterator(); it.hasNext(); ) {
                appendMarker(sb, it.next(), firstInList && !rendered, useColors);
            }
        }
    }
}

