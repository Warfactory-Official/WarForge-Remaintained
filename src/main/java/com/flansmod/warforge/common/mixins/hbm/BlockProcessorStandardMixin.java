package com.flansmod.warforge.common.mixins.hbm;

import com.flansmod.warforge.common.ExplosionProtection;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

// Covers the modern HBM "vanillant" (ExplosionVNT) family used by most NTM explosives.
// The igniter is ExplosionVNT.exploder; protected positions are stripped from the
// affected-block set before BlockProcessorStandard destroys them.
@Mixin(value = BlockProcessorStandard.class, remap = false)
public class BlockProcessorStandardMixin {

    @Inject(method = "process", at = @At("HEAD"), remap = false)
    private void warforge$protectClaimedChunks(ExplosionVNT explosion, World world, double x, double y, double z, HashSet<BlockPos> affectedBlocks, CallbackInfo ci) {
        ExplosionProtection.filter(world, explosion.exploder, affectedBlocks);
    }
}
