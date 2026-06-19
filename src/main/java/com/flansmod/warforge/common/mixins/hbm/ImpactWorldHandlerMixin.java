package com.flansmod.warforge.common.mixins.hbm;

import com.flansmod.warforge.common.ExplosionProtection;
import com.flansmod.warforge.server.Faction;
import com.hbm.handler.ImpactWorldHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = ImpactWorldHandler.class, remap = false)
public class ImpactWorldHandlerMixin {

    @Inject(method = "die", at = @At("HEAD"), remap = false, cancellable = true)
    private static void warforge$protectDie(World world, BlockPos pos, CallbackInfo ci) {
        if (ExplosionProtection.isProtected(world, Faction.nullUuid, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "burn", at = @At("HEAD"), remap = false, cancellable = true)
    private static void warforge$protectBurn(World world, BlockPos pos, CallbackInfo ci) {
        if (ExplosionProtection.isProtected(world, Faction.nullUuid, pos)) {
            ci.cancel();
        }
    }
}
