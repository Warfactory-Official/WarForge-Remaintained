package com.flansmod.warforge.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
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

    protected void onRemoved(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {}

    protected boolean isTransparentToLight(BlockState state) {
        return false;
    }

    public static InteractionResult useWithEmptyHand(BlockState state, Level level, Player player, BlockHitResult hit) {
        return state.use(level, player, InteractionHand.MAIN_HAND, hit);
    }

    // 1.20.1 calls use() once per hand; later versions call useWithoutItem() for the main hand only.
    @Override
    @SuppressWarnings("deprecation")
    public final InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        return onUse(state, level, pos, player, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public final void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof WFBlockEntity blockEntity) {
                blockEntity.onBlockRemoved();
            }
            onRemoved(state, level, pos, newState, movedByPiston);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return isTransparentToLight(state) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return isTransparentToLight(state) || super.propagatesSkylightDown(state, level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return isTransparentToLight(state) ? 0 : super.getLightBlock(state, level, pos);
    }
}
