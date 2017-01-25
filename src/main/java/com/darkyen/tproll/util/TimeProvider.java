package com.darkyen.tproll.util;

public interface TimeProvider {

    /** Returns current time. Can be relative or absolute. MUST be thread safe. */
    long timeMillis();

    TimeProvider CURRENT_TIME_PROVIDER = System::currentTimeMillis;

    /** Creates and returns time provider, which counts time from NOW. */
    static TimeProvider relativeTimeProvider() {
        final long now = System.currentTimeMillis();
        return () -> System.currentTimeMillis() - now;
    }
}
