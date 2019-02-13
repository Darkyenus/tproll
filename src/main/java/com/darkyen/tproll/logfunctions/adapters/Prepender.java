package com.darkyen.tproll.logfunctions.adapters;

import com.darkyen.tproll.LogFunction;
import org.slf4j.Marker;

/**
 * LogFunction adapter which prepends given message to each line of logged message
 */
@SuppressWarnings("unused")
public class Prepender extends LogFunction {

    private final LogFunction parent;
    private final String prepend;

    public Prepender(LogFunction parent, String prepend) {
        this.parent = parent;
        this.prepend = prepend;
    }

    @Override
    public void log(String name, long time, byte level, Marker marker, CharSequence content) {
        final int contentLen = content.length();
        final StringBuilder sb = new StringBuilder(prepend.length() << 2 + contentLen);
        sb.append(prepend);
        for (int i = 0; i < contentLen; i++) {
            final char c = content.charAt(i);
            sb.append(c);
            if (c == '\n') {
                sb.append(prepend);
            }
        }
        parent.log(name, time, level, marker, sb);
    }

    @Override
    public boolean isEnabled(byte level, Marker marker) {
        return parent.isEnabled(level, marker);
    }
}
