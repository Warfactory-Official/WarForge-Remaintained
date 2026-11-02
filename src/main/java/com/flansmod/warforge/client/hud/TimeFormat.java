package com.flansmod.warforge.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;

public final class TimeFormat {
    private static final long MS_PER_SECOND = 1000L;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;

    private TimeFormat() {
    }

    public static String formatPaddedTimer(long msRemaining) {
        long totalSeconds = Math.max(0L, msRemaining) / MS_PER_SECOND;
        long totalMinutes = totalSeconds / SECONDS_PER_MINUTE;
        long totalHours = totalMinutes / MINUTES_PER_HOUR;
        long days = totalHours / HOURS_PER_DAY;

        StringBuilder out = new StringBuilder(16);
        if (days > 0) {
            out.append(days).append(" days, ");
        }
        appendPadded(out, totalHours % HOURS_PER_DAY).append(':');
        appendPadded(out, totalMinutes % MINUTES_PER_HOUR).append(':');
        appendPadded(out, totalSeconds % SECONDS_PER_MINUTE);
        return out.toString();
    }

    private static StringBuilder appendPadded(StringBuilder out, long value) {
        if (value < 10) {
            out.append('0');
        }
        return out.append(value);
    }

    public static final class CachedTimer {
        private final String langKey;
        private long shownSeconds = -1L;
        private String text = "";
        private int width = -1;

        public CachedTimer(String langKey) {
            this.langKey = langKey;
        }

        public String get(long msRemaining) {
            long seconds = Math.max(0L, msRemaining) / MS_PER_SECOND;
            if (seconds != shownSeconds) {
                shownSeconds = seconds;
                String timer = formatPaddedTimer(msRemaining);
                text = langKey == null ? timer : I18n.get(langKey, timer);
                width = -1;
            }
            return text;
        }

        public int width(Font font) {
            if (width < 0) {
                width = font.width(text);
            }
            return width;
        }
    }
}
