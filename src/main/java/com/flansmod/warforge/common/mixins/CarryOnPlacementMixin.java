package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.compat.carryon.CarryOnGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Carry On placeCarried (drop-when-hit, death, rider disconnect, /carryon place) sets the block directly, no event => bypasses claims.
// Denied target => block dropped as item, carry cleared before Carry On reads it. getDeathPlacementPos runs twice; failsafe path only.
@Pseudo
@Mixin(targets = "tschipp.carryon.common.carry.PlacementHandler", remap = false)
@SuppressWarnings("unused")
public abstract class CarryOnPlacementMixin {
    @Shadow(remap = false)
    private static BlockPos getDeathPlacementPos(BlockState state, ServerPlayer player) {
        throw new AssertionError();
    }

    @Inject(method = "placeCarried(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), remap = false)
    private static void warforge$guardPlaceCarried(ServerPlayer player, CallbackInfo ci) {
        BlockState state = CarryOnGuard.carriedState(player);
        if (state != null) {
            CarryOnGuard.denyIfProtected(player, state, getDeathPlacementPos(state, player));
        }
    }
}
