package com.flansmod.warforge.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;

public final class Levels {
    private Levels() {}

    public static int minY(LevelHeightAccessor level) {
        return level.getMinY();
    }

    public static int maxYExclusive(LevelHeightAccessor level) {
        return level.getMaxY() + 1;
    }

    public static BlockPos sharedSpawnPos(ServerLevel level) {
        return level.getRespawnData().globalPos().pos();
    }
}
