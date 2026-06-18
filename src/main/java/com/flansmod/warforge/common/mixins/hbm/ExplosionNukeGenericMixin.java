package com.flansmod.warforge.common.mixins.hbm;

import com.flansmod.warforge.common.ExplosionProtection;
import com.flansmod.warforge.server.Faction;
import com.hbm.explosion.ExplosionNukeGeneric;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Per-block destruction sites shared by the legacy HBM nuke family (MK3 / Advanced / waste etc).
// These statics carry no igniter, so claimed chunks are protected anonymously: the block is
// only spared when an explosion would be denied to a non-member (claimed/safe/citadel chunks).
@Mixin(value = ExplosionNukeGeneric.class, remap = false)
public class ExplosionNukeGenericMixin {

    @Inject(method = "destruction", at = @At("HEAD"), remap = false, cancellable = true)
    private static void warforge$protectDestruction(World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (ExplosionProtection.isProtected(world, Faction.nullUuid, pos)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "vaporDest", at = @At("HEAD"), remap = false, cancellable = true)
    private static void warforge$protectVapor(World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (ExplosionProtection.isProtected(world, Faction.nullUuid, pos)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "wasteDest", at = @At("HEAD"), remap = false, cancellable = true)
    private static void warforge$protectWaste(World world, BlockPos pos, CallbackInfo ci) {
        if (ExplosionProtection.isProtected(world, Faction.nullUuid, pos)) {
            ci.cancel();
        }
    }
}
