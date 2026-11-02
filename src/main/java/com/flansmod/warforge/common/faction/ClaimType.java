package com.flansmod.warforge.common.faction;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.blocks.IClaim;
import com.flansmod.warforge.common.blocks.BasicClaimBlockEntity;
import com.flansmod.warforge.common.blocks.CitadelBlockEntity;
import com.flansmod.warforge.common.blocks.ReinforcedClaimBlockEntity;
import com.flansmod.warforge.common.blocks.SiegeCampBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

public enum ClaimType {
    NONE("none", "", () -> 0, () -> 0),
    BASIC("basic", "B", () -> WarForgeConfig.CLAIM_STRENGTH_BASIC, () -> WarForgeConfig.SUPPORT_STRENGTH_BASIC),
    REINFORCED("reinforced", "R", () -> WarForgeConfig.CLAIM_STRENGTH_REINFORCED, () -> WarForgeConfig.SUPPORT_STRENGTH_REINFORCED),
    CITADEL("citadel", "C", () -> WarForgeConfig.CLAIM_STRENGTH_CITADEL, () -> WarForgeConfig.SUPPORT_STRENGTH_CITADEL),
    // Blockless, unsiegeable zone claims; protection comes from the owning pseudo-faction, not strengths
    ADMIN("admin", "A", () -> 0, () -> 0),
    WARZONE("warzone", "W", () -> 0, () -> 0),
    SIEGE("siege", "S", () -> 0, () -> 0);

    private static final Map<String, ClaimType> BY_NAME = new HashMap<>();

    static {
        for (ClaimType type : values()) {
            BY_NAME.put(type.serializedName, type);
        }
    }

    public final String serializedName;
    public final String shortLabel;
    private final IntSupplier defenceStrength;
    private final IntSupplier supportStrength;

    ClaimType(String serializedName, String shortLabel, IntSupplier defenceStrength, IntSupplier supportStrength) {
        this.serializedName = serializedName;
        this.shortLabel = shortLabel;
        this.defenceStrength = defenceStrength;
        this.supportStrength = supportStrength;
    }

    public int defenceStrength() {
        return defenceStrength.getAsInt();
    }

    public int supportStrength() {
        return supportStrength.getAsInt();
    }

    public static ClaimType fromClaim(IClaim claim) {
        if (claim instanceof CitadelBlockEntity) return CITADEL;
        if (claim instanceof ReinforcedClaimBlockEntity) return REINFORCED;
        if (claim instanceof BasicClaimBlockEntity) return BASIC;
        if (claim instanceof SiegeCampBlockEntity) return SIEGE;
        return NONE;
    }

    public static ClaimType fromSerialized(String value) {
        return BY_NAME.getOrDefault(value, NONE);
    }
}
