package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.ExplosionProtection;
import com.flansmod.warforge.common.faction.FactionIds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// editBlock transforms (scorch/crater surroundings) setBlock per ray block, before toBlow filtering.
@Pseudo
@Mixin(targets = {
        "rbasamoyai.createbigcannons.munitions.ShellExplosion",
        "rbasamoyai.createbigcannons.munitions.ImpactExplosion",
        "rbasamoyai.createbigcannons.munitions.big_cannon.mortar_stone.MortarStoneExplosion",
}, remap = false)
public class CbcEditBlockClaimMixin {
    @Inject(method = "editBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void warforge$claimGate(Level level, BlockPos pos, BlockState state, FluidState fluid, float power, CallbackInfo ci) {
        if (ExplosionProtection.isProtected(level, FactionIds.NONE, pos)) {
            ci.cancel();
        }
    }
}
