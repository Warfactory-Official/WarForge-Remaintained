package com.flansmod.warforge.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Creates a block entity (usually the block entity's constructor). */
@FunctionalInterface
public interface BlockEntityFactory<E extends BlockEntity> {
    E create(BlockPos pos, BlockState state);
}
