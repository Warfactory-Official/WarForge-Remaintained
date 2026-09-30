package com.flansmod.warforge.client;

import com.flansmod.warforge.compat.Chunks;

import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import com.flansmod.warforge.platform.Interactions;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;


public class ClientPlacementPredictor {

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide())
            return;
        if (Interactions.isUseItemDenied(event))
            return; // already suppressed

        Player player = event.getEntity();
        if (ProtectionsModule.OP_OVERRIDE && WarForgeMod.isOp(player))
            return;

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem))
            return; // only block placement produces a phantom block to suppress

        // Resolve the cell the block would actually land in
        BlockPos placePos = new BlockPlaceContext(player, event.getHand(), stack, event.getHitVec()).getClickedPos();
        ChunkPos cp = Chunks.of(placePos);
        DimChunkPos chunkPos = new DimChunkPos(level.dimension(), Chunks.x(cp), Chunks.z(cp));

        ProtectionConfig config = ClientProtections.configFor(chunkPos);
        if (config == null)
            return; // unknown ownership: defer to the server

        Block placedBlock = blockItem.getBlock();
        if (ProtectionsModule.placeDenied(config, placedBlock))
            Interactions.denyUseItem(event);
    }
}
