package com.flansmod.warforge.common.network;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.common.ProtectionProfile;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;

import java.util.UUID;

public class ClaimChunkInfo {
    public static final byte FLAG_CAN_CLAIM = 1;
    public static final byte FLAG_CAN_UNCLAIM = 1 << 1;
    public static final byte FLAG_CAN_TOGGLE_FORCELOAD = 1 << 2;
    public static final byte FLAG_FORCE_LOADED = 1 << 3;
    public static final byte FLAG_HAS_COLLECTOR = 1 << 4;
    public static final byte FLAG_OWNED_BY_PLAYER = 1 << 5;
    public static final byte OUTLINE_NONE = 0;
    public static final byte OUTLINE_CLAIM = 1;
    public static final byte OUTLINE_CONQUERED = 2;

    public int x;
    public int z;
    public UUID factionId = FactionIds.NONE;
    public String factionName = "";
    public String flagId = "";
    public int colour = 0xFFFFFF;
    public ClaimType claimType = ClaimType.NONE;
    public Vein vein;
    public Quality oreQuality;
    public byte flags;
    public UUID outlineFactionId = FactionIds.NONE;
    public int outlineColour = 0xFFFFFF;
    public byte outlineStyle = OUTLINE_NONE;
    /** >0 only for an unclaimed conquered chunk: ms until it reverts to wilderness. */
    public int conqueredRemainingMs = 0;
    /** Server-resolved for the receiving player. */
    public ProtectionProfile protection = ProtectionProfile.UNCLAIMED;

    public boolean hasFlag(byte flag) {
        return (flags & flag) != 0;
    }

    public boolean hasVisibleOutline() {
        return outlineStyle != OUTLINE_NONE && !outlineFactionId.equals(FactionIds.NONE);
    }

    /** Hash of everything a map cell shows; the remaining conquered time counts in whole minutes. */
    public int signature() {
        int h = 31 * x + z;
        h = 31 * h + factionId.hashCode();
        h = 31 * h + flagId.hashCode();
        h = 31 * h + factionName.hashCode();
        h = 31 * h + colour;
        h = 31 * h + claimType.ordinal();
        h = 31 * h + (vein == null ? 0 : System.identityHashCode(vein));
        h = 31 * h + (oreQuality == null ? 0 : oreQuality.ordinal() + 1);
        h = 31 * h + flags;
        h = 31 * h + outlineFactionId.hashCode();
        h = 31 * h + outlineColour;
        h = 31 * h + outlineStyle;
        h = 31 * h + conqueredRemainingMs / 60000;
        return 31 * h + protection.ordinal();
    }

    /** Same border look: the mesh depends on nothing else. */
    public boolean sameOutline(ClaimChunkInfo other) {
        return outlineStyle == other.outlineStyle && outlineColour == other.outlineColour
                && outlineFactionId.equals(other.outlineFactionId);
    }
}
