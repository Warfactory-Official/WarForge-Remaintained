package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.world.entity.player.Player;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.BlockPlacerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockPlacerHelper.class, remap = false)
public class SurvivalHelperMixin {

    @Inject(method = "placeBlock", at = @At("HEAD"), remap = false, cancellable = true)
    private static void warforge$placeBlock(Player player, BlockEntry blockEntry, CallbackInfoReturnable<Boolean> cir) {

        if (ProtectionsModule.OP_OVERRIDE && player.getServer() != null
                && player.getServer().getPlayerList().isOp(player.getGameProfile())) return;

        DimBlockPos dimPos = new DimBlockPos(player.level().dimension(), blockEntry.blockPos);
        ProtectionConfig config = ProtectionsModule.GetProtections(player.getUUID(), dimPos);

        if (!config.PLACE_BLOCKS) {
            if (!config.BLOCK_PLACE_WHITELIST.contains(blockEntry.newBlockState.getBlock())) {
                cir.setReturnValue(false);
            }
        }
    }
}
