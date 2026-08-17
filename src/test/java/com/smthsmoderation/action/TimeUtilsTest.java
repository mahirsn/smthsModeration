package com.smthsmoderation.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilsTest {

    @Test
    void parsesEachUnit() {
        assertEquals(30, TimeUtils.parseMinutes("30m"));
        assertEquals(120, TimeUtils.parseMinutes("2h"));
        assertEquals(1440, TimeUtils.parseMinutes("1d"));
        assertEquals(10080, TimeUtils.parseMinutes("1w"));
        assertEquals(129600, TimeUtils.parseMinutes("3mo"));
        assertEquals(1, TimeUtils.parseMinutes("45s")); // rounds 0.75 -> 1
    }

    @Test
    void bareNumberIsMinutes() {
        assertEquals(15, TimeUtils.parseMinutes("15"));
    }

    @Test
    void invalidOrBlankReturnsZero() {
        assertEquals(0, TimeUtils.parseMinutes(""));
        assertEquals(0, TimeUtils.parseMinutes(null));
        assertEquals(0, TimeUtils.parseMinutes("abc"));
        assertEquals(0, TimeUtils.parseMinutes("30x"));
    }

    @Test
    void multiplyRoundsToNearestMinute() {
        assertEquals(42, TimeUtils.multiplyMinutes(30, 1.4));
    }

    @Test
    void formatsCompactly() {
        assertEquals("1d", TimeUtils.formatMinutes(1440));
        assertEquals("1d 2h", TimeUtils.formatMinutes(1440 + 120));
        assertEquals("1h 30m", TimeUtils.formatMinutes(90));
        assertEquals("0m", TimeUtils.formatMinutes(0));
    }
}
