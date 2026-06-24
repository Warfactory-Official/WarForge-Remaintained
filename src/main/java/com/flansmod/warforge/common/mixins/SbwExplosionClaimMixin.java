package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.ExplosionProtection;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Superb Warfare's main blast (CustomExplosion) fires ExplosionEvent.Detonate, so ProtectionsModule
// already filters it. Its explosive entities ALSO punch direct Level.destroyBlock holes ("extra
// explosion effect" / impact craters) that fire no event and therefore bypass claims. Re-route those
// through the same claim check. Targets are listed by string so this compiles without SBW on the
// classpath; missing targets (e.g. the other version's packaging) are skipped by required:false, and
// the whole mixin is only registered when SBW is present (see SbwMixinPlugin).
@Mixin(remap = false, targets = {
        // C4 and the TM-62 mine moved entity -> entity.projectile between 0.8.8 and 0.8.9; list both.
        "com.atsuishio.superbwarfare.entity.C4Entity",
        "com.atsuishio.superbwarfare.entity.Tm62Entity",
        "com.atsuishio.superbwarfare.entity.projectile.C4Entity",
        "com.atsuishio.superbwarfare.entity.projectile.Tm62Entity",
        "com.atsuishio.superbwarfare.entity.projectile.AerialBombEntity",
        "com.atsuishio.superbwarfare.entity.projectile.Agm65Entity",
        "com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity",
        "com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile",
        "com.atsuishio.superbwarfare.entity.projectile.IglaMissileEntity",
        "com.atsuishio.superbwarfare.entity.projectile.JavelinMissileEntity",
        "com.atsuishio.superbwarfare.entity.projectile.Kh39Entity",
        "com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity",
        "com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity",
        "com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity",
        "com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity",
        "com.atsuishio.superbwarfare.entity.projectile.RpgRocketTBGEntity",
        "com.atsuishio.superbwarfare.entity.projectile.Ru9m336MissileEntity",
        "com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity",
        "com.atsuishio.superbwarfare.entity.projectile.SmallRocketEntity",
        "com.atsuishio.superbwarfare.entity.projectile.WireGuideMissileEntity",
        "com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity",
})
public class SbwExplosionClaimMixin {

    // method = "*" so the redirect also catches destroyBlock calls inside the forEach lambdas SBW uses
    // (e.g. C4Entity#lambda$explode$0). require = 0 keeps it inert rather than crashing on a version
    // where one of these classes no longer performs a direct destroy.
    @Redirect(
            method = "*",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z",
                    remap = true),
            require = 0)
    private boolean warforge$guardExplosionDestroy(Level level, BlockPos pos, boolean dropBlock) {
        if (ExplosionProtection.isProtected(level, Faction.nullUuid, pos)) {
            return false;
        }
        return level.destroyBlock(pos, dropBlock);
    }
}
