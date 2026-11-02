package com.flansmod.warforge.client;

import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.platform.Interactions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class ClientPlacementPredictor {

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide() || Interactions.isUseItemDenied(event)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        Player player = event.getEntity();
        BlockPos placePos = new BlockPlaceContext(player, event.getHand(), stack, event.getHitVec()).getClickedPos();
        ProtectionConfig config = ClientProtections.configFor(level.dimension(), placePos.getX() >> 4, placePos.getZ() >> 4);
        if (config != null && ProtectionsModule.placeDenied(config, blockItem.getBlock())) {
            Interactions.denyUseItem(event);
        }
    }
}
