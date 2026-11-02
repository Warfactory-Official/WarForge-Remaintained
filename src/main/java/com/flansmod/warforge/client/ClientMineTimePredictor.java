package com.flansmod.warforge.client;

import com.flansmod.warforge.common.MineTime;
import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Never cancels a break: hard protection is the server's BreakEvent cancel; synced profile can lag a siege change by
 * one border sync.
 */
public class ClientMineTimePredictor {

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (!level.isClientSide())
            return;
        if (player.getAbilities().instabuild)
            return;
        BlockPos blockPos = event.getPosition().orElse(null);
        if (blockPos == null)
            return;

        ProtectionConfig config = ClientProtections.configFor(level.dimension(), blockPos.getX() >> 4, blockPos.getZ() >> 4);
        if (config == null)
            return; // unknown ownership: defer to the server

        Block block = event.getState().getBlock();
        if (!ProtectionsModule.breakDenied(config, block))
            return;

        MineTime.Rule rule = config.mineTime.resolve(block);
        if (rule == null)
            return; // hard-protected: leave to the server cancel
        if (event.getState().getDestroySpeed(level, blockPos) <= 0)
            return; // instant-break: cannot be paced

        event.setNewSpeed(MineTime.applySpeed(rule, event.getNewSpeed(), event.getState(), level, blockPos, player));
    }
}
