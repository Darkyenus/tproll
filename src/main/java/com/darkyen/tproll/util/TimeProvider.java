package com.darkyen.tproll.util;

import org.joda.time.DateTime;

public abstract class TimeProvider {

    /** Returns current time. Can be relative or absolute. MUST be thread safe. */
    public abstract long timeMillis();

    /** Returns absolute time, with timezone of the application. */
    public DateTime time() {
        return DateTime.now();
    }

    public static final TimeProvider CURRENT_TIME_PROVIDER = new TimeProvider() {
        @Override
        public long timeMillis() {
            return System.currentTimeMillis();
        }
    };

    /** Creates and returns time provider, which counts time from NOW. */
    static TimeProvider relativeTimeProvider() {
        final long now = System.currentTimeMillis();
        return new TimeProvider() {
            @Override
            public long timeMillis() {
                return System.currentTimeMillis() - now;
            }
        };
    }
}
