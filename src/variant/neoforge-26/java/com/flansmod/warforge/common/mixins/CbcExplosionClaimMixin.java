package com.flansmod.warforge.common.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

// TODO: 26.x Explosion exposes no getToBlow(); per-block CBC filter unported. Class must exist: GATED_MIXINS names it.
@Pseudo
@Mixin(targets = "rbasamoyai.createbigcannons.CreateBigCannons", remap = false)
public class CbcExplosionClaimMixin {
}
