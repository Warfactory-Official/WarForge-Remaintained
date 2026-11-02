package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.ExplosionProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Ash Vehicles direct Level.destroyBlock craters fire no event => bypass claims; re-routed through the claim check.
// 2-arg destroyBlock, plus 3-arg (Entity) overload for GBU-57. Targets are strings (no compile dependency).
// Package casing Aru.Aru.* (older CF builds) vs aru.aru.* (newer): both listed, missing one skipped (require=0).
// @Pseudo: targets may be missing at compile time; Forge refmap generation still maps the redirect target.
@Pseudo
@Mixin(remap = false, targets = {
        "aru.aru.ashvehicle.entity.projectile.Agm114Entity",
        "aru.aru.ashvehicle.entity.projectile.Agm158Entity",
        "aru.aru.ashvehicle.entity.projectile.Aim120Entity",
        "aru.aru.ashvehicle.entity.projectile.Aim54Entity",
        "aru.aru.ashvehicle.entity.projectile.Aim9Entity",
        "aru.aru.ashvehicle.entity.projectile.Gbu57Entity",
        "aru.aru.ashvehicle.entity.projectile.R60Entity",
        "Aru.Aru.ashvehicle.entity.projectile.Agm114Entity",
        "Aru.Aru.ashvehicle.entity.projectile.Agm158Entity",
        "Aru.Aru.ashvehicle.entity.projectile.Aim120Entity",
        "Aru.Aru.ashvehicle.entity.projectile.Aim54Entity",
        "Aru.Aru.ashvehicle.entity.projectile.Aim9Entity",
        "Aru.Aru.ashvehicle.entity.projectile.Gbu57Entity",
        "Aru.Aru.ashvehicle.entity.projectile.R60Entity",
})
@SuppressWarnings("unused")
public class AshVehicleExplosionClaimMixin {
    // require = 0: each redirect only matches the subset of targets using that overload (e.g. only Gbu57
    // uses the 3-arg form), so the other targets legitimately yield zero injection points.
    @Redirect(
            method = "*",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z",
                    remap = true),
            require = 0)
    private boolean warforge$guardDestroy(Level level, BlockPos pos, boolean dropBlock) {
        if (ExplosionProtection.isProtected(level, FactionIds.NONE, pos)) {
            return false;
        }
        return level.destroyBlock(pos, dropBlock);
    }

    @Redirect(
            method = "*",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z",
                    remap = true),
            require = 0)
    private boolean warforge$guardDestroyWithEntity(Level level, BlockPos pos, boolean dropBlock, Entity breakingEntity) {
        if (ExplosionProtection.isProtected(level, FactionIds.NONE, pos)) {
            return false;
        }
        return level.destroyBlock(pos, dropBlock, breakingEntity);
    }
}
