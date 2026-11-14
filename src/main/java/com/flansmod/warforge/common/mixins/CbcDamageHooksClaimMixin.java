package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.ExplosionProtection;
import com.flansmod.warforge.common.faction.FactionIds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// CBC fires no ExplosionEvent.Detonate. canDamageTerrain gates penetration, explosion centre, shrapnel.
@Pseudo
@Mixin(targets = "rbasamoyai.createbigcannons.munitions.ProjectileDamageHooks", remap = false)
public class CbcDamageHooksClaimMixin {
    @Inject(method = "canDamageTerrain", at = @At("HEAD"), cancellable = true, require = 0)
    private static void warforge$claimGate(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (ExplosionProtection.isProtected(level, FactionIds.NONE, pos)) {
            cir.setReturnValue(false);
        }
    }
}
