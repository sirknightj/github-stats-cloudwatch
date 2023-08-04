package com.sirknightj.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Class that holds utility methods regarding {@link java.util.Date}.
 */
public final class DateHelper {

    private DateHelper() {

    }

    /**
     * Converts a date to the ISO-8601 format, and also encodes it to be safe to use in a URL.
     * <p>
     * A date in the ISO-8601 format looks like this: 2014-02-27T15:05:06+01:00
     * <p>
     * The returned date would be: 2014-02-27T15%3A05%3A06+01%3A00
     *
     * @param date the date to convert.
     * @return the date in ISO format
     */
    public static String getFormattedAndURLEncodedDateString(final ZonedDateTime date) {
        return date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace(":", "%3A");
    }
}
