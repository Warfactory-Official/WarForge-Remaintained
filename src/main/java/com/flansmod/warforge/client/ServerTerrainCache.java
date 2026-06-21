package com.flansmod.warforge.client;

import com.flansmod.warforge.api.modularui.ChunkMapUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.concurrent.ConcurrentHashMap;

// Client cache of server-sampled terrain (vanilla MapColor + heightmap) for chunks the client has
// not loaded itself, keyed by chunk. The chunk-map texture daemon reads this to render real terrain
// for remote regions (e.g. a distant siege target chosen in the declaration UI). Populated by
// PacketTerrainColors and cleared on logout / dimension change. Thread-safe: written on the client
// main thread, read on the texture daemon thread.
public final class ServerTerrainCache {
    private static final ConcurrentHashMap<Long, int[]> COLORS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, int[]> HEIGHTS = new ConcurrentHashMap<>();
    private static volatile ResourceKey<Level> cachedDim = null;

    private ServerTerrainCache() {
    }

    public static void put(ResourceKey<Level> dim, int chunkX, int chunkZ, int[] colors, int[] heights) {
        if (!dim.equals(cachedDim)) {
            COLORS.clear();
            HEIGHTS.clear();
            cachedDim = dim;
        }
        long key = ChunkMapUtil.key(chunkX, chunkZ);
        COLORS.put(key, colors);
        HEIGHTS.put(key, heights);
    }

    /** 0xRRGGBB top-block colour at the world position, or {@link Integer#MIN_VALUE} if not cached. */
    public static int colorAt(ResourceKey<Level> dim, int worldX, int worldZ) {
        if (!dim.equals(cachedDim)) {
            return Integer.MIN_VALUE;
        }
        int[] c = COLORS.get(ChunkMapUtil.key(worldX >> 4, worldZ >> 4));
        return c == null ? Integer.MIN_VALUE : c[(worldX & 15) + (worldZ & 15) * 16];
    }

    /** Heightmap value at the world position, or {@link Integer#MIN_VALUE} if not cached. */
    public static int heightAt(ResourceKey<Level> dim, int worldX, int worldZ) {
        if (!dim.equals(cachedDim)) {
            return Integer.MIN_VALUE;
        }
        int[] h = HEIGHTS.get(ChunkMapUtil.key(worldX >> 4, worldZ >> 4));
        return h == null ? Integer.MIN_VALUE : h[(worldX & 15) + (worldZ & 15) * 16];
    }

    public static void clear() {
        COLORS.clear();
        HEIGHTS.clear();
        cachedDim = null;
    }
}
