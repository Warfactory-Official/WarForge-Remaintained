package com.flansmod.warforge.common;

import com.flansmod.warforge.api.Time;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Siege round length per momentum level, parsed from the configured {@code level=time} entries. */
public final class SiegeMomentum {
    private static volatile Map<Byte, Integer> seconds = Map.of();
    private static volatile byte max = 0;

    private SiegeMomentum() {}

    static void configure(List<String> entries) {
        HashMap<Byte, Integer> parsed = new HashMap<>();
        byte highest = 0;
        for (String entry : entries) {
            String[] split = entry.split("=");
            if (split.length != 2) {
                WarForgeMod.LOGGER.warn("Ignoring malformed siege momentum entry '{}'", entry);
                continue;
            }
            try {
                byte level = Byte.parseByte(split[0]);
                parsed.put(level, (int) (Time.fromString(split[1]).getMs() / 1000));
                highest = (byte) Math.max(highest, level);
            } catch (RuntimeException e) {
                WarForgeMod.LOGGER.warn("Ignoring siege momentum entry '{}'", entry, e);
            }
        }
        seconds = Map.copyOf(parsed);
        max = highest;
    }

    /** Round length in seconds, or null for a level the config does not define. */
    @Nullable
    public static Integer seconds(byte level) {
        return seconds.get(level);
    }

    /** All defined levels. */
    public static Map<Byte, Integer> levels() {
        return seconds;
    }

    /** Highest defined level. */
    public static byte max() {
        return max;
    }
}
