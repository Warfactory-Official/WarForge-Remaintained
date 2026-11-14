package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.ExplosionProtection;
import com.flansmod.warforge.common.faction.FactionIds;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Centre gate alone lets blasts from outside spill into claims; filter per block.
@Pseudo
@Mixin(targets = "rbasamoyai.createbigcannons.CreateBigCannons", remap = false)
public class CbcExplosionClaimMixin {
    @Inject(method = "handleCustomExplosion",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;finalizeExplosion(Z)V", remap = true),
            require = 0)
    private static void warforge$filterToBlow(Level level, Explosion explosion, CallbackInfo ci) {
        ExplosionProtection.filter(level, FactionIds.NONE, explosion.getToBlow());
    }
}
