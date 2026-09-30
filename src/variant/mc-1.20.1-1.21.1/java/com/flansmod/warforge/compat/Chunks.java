package com.flansmod.warforge.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** ChunkPos access. ChunkPos became a record in 26.1 (x()/z() accessors, no BlockPos constructor). */
public final class Chunks {
    private Chunks() {}

    public static int x(ChunkPos pos) {
        return pos.x;
    }

    public static int z(ChunkPos pos) {
        return pos.z;
    }

    /** The chunk that contains a block. */
    public static ChunkPos of(BlockPos pos) {
        return new ChunkPos(pos);
    }
}
