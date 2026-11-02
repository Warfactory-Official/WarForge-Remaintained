package com.flansmod.warforge.compat.golem;

import com.flansmod.warforge.api.AlterKind;
import com.flansmod.warforge.api.HarmKind;
import com.flansmod.warforge.api.WarforgeAPI;
import dev.golem.core.world.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Golem Core protection provider; every answer comes from {@link WarforgeAPI}. Load only when Golem Core is present. */
public final class GolemProtection implements Protection.Provider {

    public static void register() {
        Protection.register(new GolemProtection());
    }

    private static AlterKind kind(Protection.Cause cause) {
        return switch (cause) {
            case EXPLOSION -> AlterKind.EXPLOSION;
            case FIRE -> AlterKind.FIRE;
            case FLUID -> AlterKind.FLUID;
            case RADIATION_BLOCK_CONVERSION -> AlterKind.RADIATION;
            case TERRAFORM -> AlterKind.TERRAFORM;
            case PLACEMENT -> AlterKind.PLACEMENT;
        };
    }

    private static HarmKind kind(Protection.Hazard hazard) {
        return switch (hazard) {
            case RADIATION -> HarmKind.RADIATION;
            case CONTAMINATION -> HarmKind.CONTAMINATION;
            case GAS -> HarmKind.GAS;
            case FIRE -> HarmKind.FIRE;
            case BLAST -> HarmKind.BLAST;
        };
    }

    @Override
    public Protection.Coverage coverage(Level level, int chunkX, int chunkZ, Protection.Source source, Protection.Cause cause) {
        if (level.isClientSide()) return Protection.Coverage.OPEN;
        return WarforgeAPI.mayAlter(level.dimension(), new ChunkPos(chunkX, chunkZ), source.player(), source.faction(), kind(cause))
                ? Protection.Coverage.OPEN : Protection.Coverage.CLOSED;
    }

    @Override
    public boolean mayAlter(Level level, BlockPos pos, Protection.Source source, Protection.Cause cause) {
        return level.isClientSide() || WarforgeAPI.mayAlter(level.dimension(), new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), source.player(), source.faction(), kind(cause));
    }

    @Override
    public boolean mayHarm(Level level, Entity victim, Protection.Source source, Protection.Hazard hazard) {
        return level.isClientSide() || WarforgeAPI.mayHarm(level, victim, source.player(), source.faction(), kind(hazard));
    }
}
