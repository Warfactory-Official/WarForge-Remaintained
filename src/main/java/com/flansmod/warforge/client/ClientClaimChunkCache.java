package com.flansmod.warforge.client;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.util.DimChunkPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.UUID;

/** Dense claim-manager window; outline-only sync lives in {@link ClientBorderCache}. */
public final class ClientClaimChunkCache {
    private static final Long2ObjectOpenHashMap<ClaimChunkInfo> CHUNKS = new Long2ObjectOpenHashMap<>();

    private static ResourceKey<Level> dim = Level.OVERWORLD;
    private static UUID playerFactionId = FactionIds.NONE;
    private static int forceLoadedCount;
    private static int forceLoadedMax;
    private static int claimCount;
    private static int claimMax;

    private ClientClaimChunkCache() {
    }

    public static void replaceAll(ResourceKey<Level> newDim, int forced, int forcedMax, int numClaims, int maxClaims,
                                  Collection<ClaimChunkInfo> chunks) {
        CHUNKS.clear();
        for (ClaimChunkInfo info : chunks) {
            CHUNKS.put(ClientBorderCache.key(info.x, info.z), info);
        }
        dim = newDim;
        forceLoadedCount = forced;
        forceLoadedMax = forcedMax;
        claimCount = numClaims;
        claimMax = maxClaims;
    }

    public static void clear() {
        CHUNKS.clear();
        dim = Level.OVERWORLD;
        playerFactionId = FactionIds.NONE;
        forceLoadedCount = 0;
        forceLoadedMax = 0;
        claimCount = 0;
        claimMax = 0;
    }

    public static ClaimChunkInfo get(ResourceKey<Level> chunkDim, int x, int z) {
        return dim.equals(chunkDim) ? CHUNKS.get(ClientBorderCache.key(x, z)) : null;
    }

    public static ClaimChunkInfo get(DimChunkPos pos) {
        return get(pos.dim, pos.x, pos.z);
    }

    public static UUID playerFactionId() {
        return playerFactionId;
    }

    public static void setPlayerFactionId(UUID factionId) {
        playerFactionId = factionId;
    }

    public static boolean playerHasFaction() {
        return FactionIds.isValid(playerFactionId);
    }

    public static int forceLoadedCount() {
        return forceLoadedCount;
    }

    public static int forceLoadedMax() {
        return forceLoadedMax;
    }

    public static int claimCount() {
        return claimCount;
    }

    public static int claimMax() {
        return claimMax;
    }
}
