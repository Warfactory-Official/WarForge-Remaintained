package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

//   Anti Obama system
//    Prevents liquid flow into claimed chunks from claimed chunks, allows for flow out of claimed chunks into unclaimed


@Mixin(FlowingFluid.class)
public class FluidClaimBorderMixin {

    @Inject(method = "canSpreadTo", at = @At("HEAD"), cancellable = true)
    private void warforge$blockForeignInflow(BlockGetter level, BlockPos fromPos, BlockState fromState,
                                             Direction direction, BlockPos toPos, BlockState toState,
                                             FluidState toFluid, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (!WarForgeConfig.BLOCK_FOREIGN_FLUID_INFLOW)
            return;
        // Same chunk => same claim => can never be a cross-border cast. Cheapest possible early-out.
        if ((fromPos.getX() >> 4) == (toPos.getX() >> 4) && (fromPos.getZ() >> 4) == (toPos.getZ() >> 4))
            return;
        if (!(level instanceof Level lvl) || lvl.isClientSide)
            return;

        ResourceKey<Level> dim = lvl.dimension();
        UUID toOwner = WarForgeMod.FACTIONS.getClaim(new DimChunkPos(dim, toPos));
        if (toOwner.equals(Faction.nullUuid))
            return; // flowing into unclaimed land is fine
        UUID fromOwner = WarForgeMod.FACTIONS.getClaim(new DimChunkPos(dim, fromPos));
        if (toOwner.equals(fromOwner))
            return; // a claim's own fluid may flow between its own chunks
        cir.setReturnValue(false); // foreign source flowing into a claim -- block it
    }
}
