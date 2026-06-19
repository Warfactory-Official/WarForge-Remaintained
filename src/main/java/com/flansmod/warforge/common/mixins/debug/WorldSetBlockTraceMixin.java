package com.flansmod.warforge.common.mixins.debug;

import com.flansmod.warforge.common.SetBlockTracer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Optional diagnostic mixin. Every block change in the world funnels through the 3-arg
// World#setBlockState(BlockPos, IBlockState, int) (the 2-arg overload delegates to it), so a
// single HEAD hook here observes all terrain mutations - including those from mods that place /
// remove blocks directly (GregTech Industrial TNT, AE2 Tiny TNT, ...) and therefore never reach
// the ExplosionEvent.Detonate handler. The actual filtering and logging lives in SetBlockTracer
// and is gated behind WarForgeConfig.DEBUG_TRACE_SETBLOCK, so this is a no-op when disabled.
@Mixin(World.class)
public abstract class WorldSetBlockTraceMixin {

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z", at = @At("HEAD"))
    private void warforge$traceSetBlockState(BlockPos pos, IBlockState newState, int flags, CallbackInfoReturnable<Boolean> cir) {
        SetBlockTracer.trace((World) (Object) this, pos, newState);
    }
}
