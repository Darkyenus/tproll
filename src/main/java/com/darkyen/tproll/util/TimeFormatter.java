package com.darkyen.tproll.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 *
 */
public interface TimeFormatter {
    void format(long millis, StringBuilder result);

    class AbsoluteTimeFormatter implements TimeFormatter {

        public static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER  = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE,2)
            .toFormatter();

        private final DateTimeFormatter formatter;

        public AbsoluteTimeFormatter(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        public AbsoluteTimeFormatter() {
            this(DEFAULT_DATE_TIME_FORMATTER);
        }

        @Override
        public void format(long millis, StringBuilder result) {
            formatter.formatTo(LocalDateTime.ofEpochSecond(millis/1000, (int)((millis%1000) * 1_000_000), ZoneOffset.UTC), result);
        }
    }

    class RelativeTimeFormatter implements TimeFormatter {

        private final boolean days, hours, minutes, seconds, milliseconds;

        public RelativeTimeFormatter(boolean days, boolean hours, boolean minutes, boolean seconds, boolean milliseconds) {
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
            this.milliseconds = milliseconds;
        }

        @Override
        public void format(long millis, StringBuilder result) {
            boolean dirty = false;

            if (days) {
                final long dayMillis = 1000L * 60 * 60 * 24;
                result.append(millis / dayMillis);
                millis %= dayMillis;
                dirty = true;
            }
            if (hours) {
                if (dirty) result.append(':');
                final long hourMillis = 1000L * 60 * 60;
                final long hours = millis / hourMillis;
                if (hours < 10 && days) {
                    result.append('0');
                }
                result.append(hours);
                millis %= hourMillis;
                dirty = true;
            }
            if (minutes) {
                if (dirty) result.append(':');
                final long minutesMillis = 1000L * 60;
                final long minutes = millis / minutesMillis;
                if (minutes < 10 && hours) {
                    result.append('0');
                }
                result.append(minutes);
                millis %= minutesMillis;
                dirty = true;
            }
            if (seconds) {
                if (dirty) result.append(':');
                final long secondsMillis = 1000L;
                final long seconds = millis / secondsMillis;
                if (seconds < 10 && minutes) {
                    result.append('0');
                }
                result.append(seconds);
                millis %= secondsMillis;
                dirty = true;
            }
            if (milliseconds) {
                if (dirty) result.append(':');
                if (seconds) {
                    if (millis < 1000) {
                        result.append('0');
                    }
                    if (millis < 100) {
                        result.append('0');
                    }
                    if (millis < 10) {
                        result.append('0');
                    }
                }
                result.append(millis);
            }
        }
    }
}