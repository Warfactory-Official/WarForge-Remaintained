package com.flansmod.warforge.client;

import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientClaimChunkCache {
    private static final HashMap<DimChunkPos, ClaimChunkInfo> CHUNKS = new HashMap<DimChunkPos, ClaimChunkInfo>();

    public static UUID playerFactionId = Faction.nullUuid;
    public static int forceLoadedCount = 0;
    public static int forceLoadedMax = 0;
    public static int claimCount = 0;
    public static int claimMax = 0;
    public static int centerDim = 0;
    public static int centerX = 0;
    public static int centerZ = 0;
    public static int radius = 0;

    public static void replaceAll(int dim, int x, int z, int newRadius, UUID factionId, int forced, int forcedMax, int numClaims, int maxClaims, Collection<ClaimChunkInfo> chunks) {
        CHUNKS.clear();
        for (ClaimChunkInfo info : chunks) {
            CHUNKS.put(new DimChunkPos(dim, info.x, info.z), info);
        }
        centerDim = dim;
        centerX = x;
        centerZ = z;
        radius = newRadius;
        playerFactionId = factionId;
        forceLoadedCount = forced;
        forceLoadedMax = forcedMax;
        claimCount = numClaims;
        claimMax = maxClaims;
    }

    public static Map<DimChunkPos, ClaimChunkInfo> getChunks() {
        return CHUNKS;
    }

    public static ClaimChunkInfo get(DimChunkPos pos) {
        return CHUNKS.get(pos);
    }
}
