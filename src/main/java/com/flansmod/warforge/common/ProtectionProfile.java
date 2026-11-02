package com.flansmod.warforge.common;

import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;

/** Wire id of the protection config {@link ProtectionsModule#GetProtections} resolved for a viewer and chunk. */
public enum ProtectionProfile {
    UNCLAIMED,
    SAFE_ZONE,
    WAR_ZONE,
    CITADEL_FRIEND,
    CITADEL_FOE,
    CLAIM_FRIEND,
    CLAIM_ALLY,
    CLAIM_FOE,
    CLAIM_DEFENDED,
    SIEGED_FRIEND,
    SIEGED_FOE,
    WAR_FRIEND,
    WAR_FOE,
    /** Operator with the protection override on: nothing denies them, so clients predict nothing. */
    UNRESTRICTED,
    /** Only the owner is known (whole-world map data): clients predict nothing. */
    UNKNOWN;

    private static final ProtectionProfile[] VALUES = values();

    /** Null for {@link #UNRESTRICTED} and {@link #UNKNOWN}. */
    public ProtectionConfig config() {
        return switch (this) {
            case UNCLAIMED -> WarForgeConfig.UNCLAIMED;
            case SAFE_ZONE -> WarForgeConfig.SAFE_ZONE;
            case WAR_ZONE -> WarForgeConfig.WAR_ZONE;
            case CITADEL_FRIEND -> WarForgeConfig.CITADEL_FRIEND;
            case CITADEL_FOE -> WarForgeConfig.CITADEL_FOE;
            case CLAIM_FRIEND -> WarForgeConfig.CLAIM_FRIEND;
            case CLAIM_ALLY -> WarForgeConfig.CLAIM_ALLY;
            case CLAIM_FOE -> WarForgeConfig.CLAIM_FOE;
            case CLAIM_DEFENDED -> WarForgeConfig.CLAIM_DEFENDED;
            case SIEGED_FRIEND -> WarForgeConfig.SIEGED_FRIEND;
            case SIEGED_FOE -> WarForgeConfig.SIEGED_FOE;
            case WAR_FRIEND -> WarForgeConfig.WAR_FRIEND;
            case WAR_FOE -> WarForgeConfig.WAR_FOE;
            case UNRESTRICTED, UNKNOWN -> null;
        };
    }

    /** The profile whose live config is this instance. */
    public static ProtectionProfile of(ProtectionConfig config) {
        for (ProtectionProfile profile : VALUES) {
            if (profile.config() == config) {
                return profile;
            }
        }
        throw new IllegalArgumentException("Not a protection profile config");
    }

    public static ProtectionProfile fromId(int id) {
        return VALUES[id];
    }
}
