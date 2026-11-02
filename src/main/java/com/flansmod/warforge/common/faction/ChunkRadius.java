package com.flansmod.warforge.common.faction;

import com.flansmod.warforge.common.util.DimChunkPos;

public final class ChunkRadius {
    private ChunkRadius() {
    }

    /** Chebyshev distance {@code <= radius} in the same dimension. */
    public static boolean contains(DimChunkPos center, DimChunkPos chunk, int radius) {
        return chunk.dim.equals(center.dim)
                && Math.abs(chunk.x - center.x) <= radius
                && Math.abs(chunk.z - center.z) <= radius;
    }
}
