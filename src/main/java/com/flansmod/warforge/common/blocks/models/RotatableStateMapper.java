package com.flansmod.warforge.common.blocks.models;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Maps a {@link BlockState} carrying a {@link DirectionProperty} to the {@code facing=*} blockstate
 * variant model location. 1.20.1 has no {@code IStateMapper}; horizontal rotation is normally driven
 * by blockstate JSON variants + {@code getStateForPlacement}. This is retained for code paths that
 * need to resolve a facing variant model location directly (e.g. the dynamic claim baked models).
 */
public class RotatableStateMapper {
    private final ResourceLocation blockName;
    private final DirectionProperty propertyDirection;

    public RotatableStateMapper(ResourceLocation blockName) {
        this(blockName, HorizontalDirectionalBlock.FACING);
    }

    public RotatableStateMapper(ResourceLocation blockName, DirectionProperty propertyDirection) {
        this.blockName = blockName;
        this.propertyDirection = propertyDirection;
    }

    public ModelResourceLocation getModelResourceLocation(BlockState state) {
        Direction facing = state.getValue(propertyDirection);
        return new ModelResourceLocation(blockName, "facing=" + facing.getName());
    }
}
