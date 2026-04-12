package com.flansmod.warforge.common.network;

import com.flansmod.warforge.server.Faction;

import java.util.UUID;

public class ClaimChunkInfo {
    public static final byte FLAG_CAN_CLAIM = 1;
    public static final byte FLAG_CAN_UNCLAIM = 1 << 1;
    public static final byte FLAG_CAN_TOGGLE_FORCELOAD = 1 << 2;
    public static final byte FLAG_FORCE_LOADED = 1 << 3;
    public static final byte FLAG_HAS_COLLECTOR = 1 << 4;
    public static final byte FLAG_OWNED_BY_PLAYER = 1 << 5;

    public int x;
    public int z;
    public UUID factionId = Faction.nullUuid;
    public String factionName = "";
    public int colour = 0xFFFFFF;
    public byte flags;

    public boolean hasFlag(byte flag) {
        return (flags & flag) != 0;
    }
}
