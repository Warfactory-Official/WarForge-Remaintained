package com.flansmod.warforge.common.protection;

import com.flansmod.warforge.common.util.DimChunkPos;

import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class FluidClaimRules {
    private FluidClaimRules() {}

    /** True if a fluid must not flow from fromPos into toPos: the target chunk's profile, seen as the source chunk's owner. */
    public static boolean blocksFlow(BlockGetter level, BlockPos fromPos, BlockPos toPos) {
        if ((fromPos.getX() >> 4) == (toPos.getX() >> 4) && (fromPos.getZ() >> 4) == (toPos.getZ() >> 4))
            return false;
        if (!(level instanceof Level lvl) || lvl.isClientSide())
            return false;

        ResourceKey<Level> dim = lvl.dimension();
        UUID fromOwner = WarForgeMod.FACTIONS.claims.getClaim(new DimChunkPos(dim, fromPos));
        DimChunkPos toChunk = new DimChunkPos(dim, toPos);
        return !WarForgeMod.FACTIONS.claims.getClaim(toChunk).equals(fromOwner)
                && !ProtectionsModule.getProtectionsForFaction(fromOwner, toChunk).ALLOW_FOREIGN_FLUID_INFLOW;
    }
}
