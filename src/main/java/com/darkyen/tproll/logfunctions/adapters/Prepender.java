package com.darkyen.tproll.logfunctions.adapters;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.logfunctions.AbstractAdapterLogFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Marker;

/**
 * LogFunction adapter which prepends given message to each line of logged message
 */
@SuppressWarnings("unused")
public final class Prepender extends AbstractAdapterLogFunction {

    private final @NotNull String prepend;

    public Prepender(@NotNull LogFunction parent, @NotNull String prepend) {
        super(parent);
        this.prepend = prepend;
    }

    @Override
    public void log(@NotNull String name, long time, byte level, @Nullable Marker marker, @NotNull CharSequence content) {
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
}
