package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.ExplosionProtection;
import com.flansmod.warforge.common.faction.FactionIds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Fluid-shell fire/ignition writes blocks directly, no event.
@Pseudo
@Mixin(targets = "rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.DefaultFluidCompat", remap = false)
public class CbcFluidFireClaimMixin {
    @Redirect(method = "spawnFire",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", remap = true),
            require = 0)
    private static boolean warforge$guardFire(Level level, BlockPos pos, BlockState state) {
        return !ExplosionProtection.isProtected(level, FactionIds.NONE, pos) && level.setBlockAndUpdate(pos, state);
    }

    @Redirect(method = "spawnFire",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", remap = true),
            require = 0)
    private static boolean warforge$guardLight(Level level, BlockPos pos, BlockState state, int flags) {
        return !ExplosionProtection.isProtected(level, FactionIds.NONE, pos) && level.setBlock(pos, state, flags);
    }
}
