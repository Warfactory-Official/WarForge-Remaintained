package com.flansmod.warforge.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class WFBlock extends Block {
    protected WFBlock(Properties properties) {
        super(properties);
    }

    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    /** Block entity gone or about to go; logic needing it goes in {@link WFBlockEntity#onBlockRemoved()}. */
    protected void onRemoved(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {}

    protected boolean isTransparentToLight(BlockState state) {
        return false;
    }

    public static InteractionResult useWithEmptyHand(BlockState state, Level level, Player player, BlockHitResult hit) {
        return state.useWithoutItem(level, player, hit);
    }

    @Override
    protected final InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onUse(state, level, pos, player, hit);
    }

    @Override
    protected final void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        onRemoved(state, level, pos, level.getBlockState(pos), movedByPiston);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return isTransparentToLight(state) ? Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return isTransparentToLight(state) || super.propagatesSkylightDown(state);
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return isTransparentToLight(state) ? 0 : super.getLightDampening(state);
    }
}
