package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.PushReaction;

import java.util.Map;

import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;

public abstract class MultiBlockColumn extends Block implements IMultiBlockInit {

    protected Map<BlockState, Vec3i> multiBlockMap;

    public MultiBlockColumn(Properties properties) {
        super(properties);
        IMultiBlockInit.INSTANCES.add(this);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        for (Vec3i relativePos : multiBlockMap.values())
            if (!world.isEmptyBlock(pos.offset(relativePos))) return false;
        return true;

    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }


    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        setUpMultiblock(world, pos, state);
    }

    public void setUpMultiblock(Level world, BlockPos pos, BlockState state) {
        world.setBlock(pos.above(), Content.statue.defaultBlockState().setValue(MODEL, BlockDummy.modelEnum.KNIGHT), 3);
        world.sendBlockUpdated(pos.above(), state, state, 3);

        BlockEntity teMiddle = world.getBlockEntity(pos.above());
        if (teMiddle instanceof TileEntityDummy) {
            ((TileEntityDummy) teMiddle).setMaster(pos);
        }

        world.setBlock(pos.above(2), Content.dummyTranslusent.defaultBlockState().setValue(MODEL, BlockDummy.modelEnum.TRANSLUCENT), 3);
        world.sendBlockUpdated(pos.above(2), state, state, 3);

        BlockEntity teTop = world.getBlockEntity(pos.above(2));
        if (teTop instanceof TileEntityDummy) {
            ((TileEntityDummy) teTop).setMaster(pos);
        }
    }


    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        for (Vec3i offset : multiBlockMap.values()) {
            BlockPos offsetPos = pos.offset(offset);
            worldIn.removeBlock(offsetPos, false);
        }
        super.onRemove(state, worldIn, pos, newState, isMoving);
    }
}
