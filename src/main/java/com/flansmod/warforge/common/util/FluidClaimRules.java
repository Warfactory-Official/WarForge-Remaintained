package com.flansmod.warforge.common.util;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * The claim rule for flowing fluids, used by FluidClaimBorderMixin (whose injection point differs between versions):
 * fluid may not flow into a claim from outside it, but may flow out of a claim into unclaimed land.
 */
public final class FluidClaimRules {
    private FluidClaimRules() {}

    /** True if a fluid must not flow from fromPos into toPos. */
    public static boolean blocksFlow(BlockGetter level, BlockPos fromPos, BlockPos toPos) {
        if (!WarForgeConfig.BLOCK_FOREIGN_FLUID_INFLOW)
            return false;
        // Same chunk => same claim => can never be a cross-border cast. Cheapest possible early-out.
        if ((fromPos.getX() >> 4) == (toPos.getX() >> 4) && (fromPos.getZ() >> 4) == (toPos.getZ() >> 4))
            return false;
        if (!(level instanceof Level lvl) || lvl.isClientSide())
            return false;

        ResourceKey<Level> dim = lvl.dimension();
        UUID toOwner = WarForgeMod.FACTIONS.getClaim(new DimChunkPos(dim, toPos));
        if (toOwner.equals(Faction.nullUuid))
            return false; // flowing into unclaimed land is fine
        UUID fromOwner = WarForgeMod.FACTIONS.getClaim(new DimChunkPos(dim, fromPos));
        if (toOwner.equals(fromOwner))
            return false; // a claim's own fluid may flow between its own chunks
        return true; // foreign source flowing into a claim -- block it
    }
}
