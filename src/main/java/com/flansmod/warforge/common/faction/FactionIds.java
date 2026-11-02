package com.flansmod.warforge.common.faction;

import java.util.UUID;

public final class FactionIds {
    public static final UUID NONE = new UUID(0, 0);
    public static final UUID SAFE_ZONE = createUUID("safezone");
    public static final UUID WAR_ZONE = createUUID("conquered zone");

    private FactionIds() {
    }

    public static UUID createUUID(String factionName) {
        return new UUID(0xfedcba0987654321L, ((long) factionName.hashCode()) * 0xa0a0b1b1c2c2d3d3L);
    }

    public static boolean isNeutralZone(UUID factionId) {
        return SAFE_ZONE.equals(factionId) || WAR_ZONE.equals(factionId);
    }

    /** False for null and {@link #NONE}. */
    public static boolean isValid(UUID factionId) {
        return factionId != null && !NONE.equals(factionId);
    }
}
