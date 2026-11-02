package com.flansmod.warforge.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;

/** Method names and top inclusivity differ per version. */
public final class Levels {
    private Levels() {}

    public static int minY(LevelHeightAccessor level) {
        return level.getMinBuildHeight();
    }

    public static int maxYExclusive(LevelHeightAccessor level) {
        return level.getMaxBuildHeight();
    }

    public static BlockPos sharedSpawnPos(ServerLevel level) {
        return level.getSharedSpawnPos();
    }
}
