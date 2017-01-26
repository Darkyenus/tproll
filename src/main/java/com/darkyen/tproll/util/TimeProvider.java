package com.darkyen.tproll.util;

import java.time.ZonedDateTime;

public interface TimeProvider {

    /** Returns current time. Can be relative or absolute. MUST be thread safe. */
    long timeMillis();

    /** Returns absolute time, with timezone of the application. */
    default ZonedDateTime time() {
        return ZonedDateTime.now();
    }

    TimeProvider CURRENT_TIME_PROVIDER = System::currentTimeMillis;

    /** Creates and returns time provider, which counts time from NOW. */
    static TimeProvider relativeTimeProvider() {
        final long now = System.currentTimeMillis();
        return () -> System.currentTimeMillis() - now;
    }
}
