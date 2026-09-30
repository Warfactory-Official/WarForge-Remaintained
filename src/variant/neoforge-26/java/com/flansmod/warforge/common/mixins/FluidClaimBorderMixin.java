package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.util.FluidClaimRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//   Anti Obama system
//    Prevents liquid flow into claimed chunks from claimed chunks, allows for flow out of claimed chunks into unclaimed

/**
 * 26.x has no canSpreadTo: spreadToSides now calls spreadTo for every direction that getSpread returns. Before, each
 * of those calls was guarded by canSpreadTo, so cancelling spreadTo here blocks exactly the same flows (the
 * downward call never crosses a chunk border).
 */
@Mixin(FlowingFluid.class)
public class FluidClaimBorderMixin {

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void warforge$blockForeignInflow(LevelAccessor level, BlockPos pos, BlockState state, Direction direction,
                                             FluidState target, CallbackInfo ci) {
        if (FluidClaimRules.blocksFlow(level, pos.relative(direction.getOpposite()), pos))
            ci.cancel(); // foreign source flowing into a claim -- block it
    }
}
