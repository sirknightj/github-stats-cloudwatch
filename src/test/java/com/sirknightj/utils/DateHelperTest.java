package com.sirknightj.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateHelperTest {

    @Test
    public void when_getFormattedDateString_then_correctFormattedCorrectly() {
        final String expectedFormattedDate = "2023-07-17T09%3A59%3A59-07%3A00";
        final ZonedDateTime testCase = LocalDateTime.of(2023, 7, 17, 9, 59, 59).atZone(ZoneId.of(ZoneId.SHORT_IDS.get("PST")));
        final String actualFormattedDate = DateHelper.getFormattedAndURLEncodedDateString(testCase);

        assertEquals(expectedFormattedDate, actualFormattedDate);
    }
}
