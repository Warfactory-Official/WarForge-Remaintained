package com.flansmod.warforge.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatTest {
    @Test
    void padsEveryField() {
        assertEquals("00:00:00", TimeFormat.formatPaddedTimer(0));
        assertEquals("01:02:03", TimeFormat.formatPaddedTimer((3600 + 2 * 60 + 3) * 1000L));
    }

    @Test
    void daysPrefixOnlyWhenPositive() {
        assertEquals("23:59:59", TimeFormat.formatPaddedTimer((86_400 - 1) * 1000L));
        assertEquals("2 days, 00:00:01", TimeFormat.formatPaddedTimer((2 * 86_400 + 1) * 1000L));
    }

    @Test
    void negativeReadsAsZero() {
        assertEquals("00:00:00", TimeFormat.formatPaddedTimer(-5000));
    }
}
