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

/**
 * Base class of every WarForge block. The vanilla use/removal/light methods differ between versions; this class
 * maps them to the hooks below.
 */
public abstract class WFBlock extends Block {
    protected WFBlock(Properties properties) {
        super(properties);
    }

    /** Right-click on the block (main hand, when the held item did not handle the click). */
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    /**
     * Server side: the block was replaced by a different block. The block entity (if any) is gone or about to go;
     * logic that needs it goes in {@link WFBlockEntity#onBlockRemoved()}.
     */
    protected void onRemoved(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {}

    /** True for a block with a full shape that must not block or occlude light (sky light passes, no dampening). */
    protected boolean isTransparentToLight(BlockState state) {
        return false;
    }

    /** Right-clicks the block at the hit position like a player with an empty main hand. */
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
