package com.flansmod.warforge.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;

/** World height limits. The method names (and whether the top is inclusive) differ between versions. */
public final class Levels {
    private Levels() {}

    /** The lowest block Y (inclusive). */
    public static int minY(LevelHeightAccessor level) {
        return level.getMinY();
    }

    /** One above the highest block Y (exclusive top). getMaxY() is the highest block Y itself. */
    public static int maxYExclusive(LevelHeightAccessor level) {
        return level.getMaxY() + 1;
    }

    /** The world spawn point of a level. */
    public static BlockPos sharedSpawnPos(ServerLevel level) {
        return level.getRespawnData().globalPos().pos();
    }
}
