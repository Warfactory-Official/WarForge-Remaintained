package com.flansmod.warforge.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** ChunkPos is a record from 26.1: x()/z(), no BlockPos constructor. */
public final class Chunks {
    private Chunks() {}

    public static int x(ChunkPos pos) {
        return pos.x;
    }

    public static int z(ChunkPos pos) {
        return pos.z;
    }

    public static ChunkPos of(BlockPos pos) {
        return new ChunkPos(pos);
    }
}
