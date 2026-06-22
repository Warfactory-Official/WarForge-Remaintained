package com.flansmod.warforge.client;

import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Sparse, wide cache of claim chunks that carry a visible outline, kept separately from
 * {@link ClientClaimChunkCache} (which is the dense, small claim-manager window). The passive border
 * sync fills this over the player's render distance, so in-world borders and the area toast can read
 * far without flooding the server with the heavy per-chunk claim-manager packet.
 */
public class ClientBorderCache {
    private static final HashMap<DimChunkPos, ClaimChunkInfo> CHUNKS = new HashMap<>();

    public static void replaceAll(ResourceKey<Level> dim, Collection<ClaimChunkInfo> chunks) {
        CHUNKS.clear();
        for (ClaimChunkInfo info : chunks) {
            CHUNKS.put(new DimChunkPos(dim, info.x, info.z), info);
        }
    }

    public static Map<DimChunkPos, ClaimChunkInfo> getChunks() {
        return CHUNKS;
    }

    public static ClaimChunkInfo get(DimChunkPos pos) {
        return CHUNKS.get(pos);
    }

    public static void clear() {
        CHUNKS.clear();
    }
}
