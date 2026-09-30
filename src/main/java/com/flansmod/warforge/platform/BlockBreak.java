package com.flansmod.warforge.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** A player is about to break a block. Cancel to prevent it. */
public interface BlockBreak {
    LevelAccessor getLevel();

    BlockPos getPos();

    BlockState getState();

    Player getPlayer();

    void setCanceled(boolean canceled);
}
