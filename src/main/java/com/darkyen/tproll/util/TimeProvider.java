package com.darkyen.tproll.util;

import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public abstract class TimeProvider {

    /** Returns current time. Can be relative or absolute. MUST be thread safe. */
    public abstract long timeMillis();

    /** Returns absolute time, with timezone of the application. */
    public @NotNull ZonedDateTime time() {
        return ZonedDateTime.now(timeZone());
    }

    /** Returns time zone that is used for all time-zone related operations */
    public @NotNull ZoneId timeZone() {
        return ZoneId.systemDefault();
    }

    public static final @NotNull TimeProvider CURRENT_TIME_PROVIDER = new TimeProvider() {
        @Override
        public long timeMillis() {
            return System.currentTimeMillis();
        }
    };

    /** Creates and returns time provider, which counts time from NOW. */
    static @NotNull TimeProvider relativeTimeProvider() {
        final long now = System.currentTimeMillis();
        return new TimeProvider() {
            @Override
            public long timeMillis() {
                return System.currentTimeMillis() - now;
            }
        };
    }
}
