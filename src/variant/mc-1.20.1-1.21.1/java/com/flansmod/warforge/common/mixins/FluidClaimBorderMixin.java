package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.util.FluidClaimRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//   Anti Obama system
//    Prevents liquid flow into claimed chunks from claimed chunks, allows for flow out of claimed chunks into unclaimed


@Mixin(FlowingFluid.class)
public class FluidClaimBorderMixin {

    @Inject(method = "canSpreadTo", at = @At("HEAD"), cancellable = true)
    private void warforge$blockForeignInflow(BlockGetter level, BlockPos fromPos, BlockState fromState,
                                             Direction direction, BlockPos toPos, BlockState toState,
                                             FluidState toFluid, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (FluidClaimRules.blocksFlow(level, fromPos, toPos))
            cir.setReturnValue(false); // foreign source flowing into a claim -- block it
    }
}
