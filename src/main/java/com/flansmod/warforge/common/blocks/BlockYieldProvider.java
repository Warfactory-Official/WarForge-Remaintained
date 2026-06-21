package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.api.IItemYieldProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// An unharvestable resource block that contributes to the yield of any faction that claims it
public class BlockYieldProvider extends Block implements IItemYieldProvider
{
	public ItemStack yieldToProvide = ItemStack.EMPTY;
	public float multiplier = 1.0f;

	public BlockYieldProvider(ItemStack yieldStack, float multiplier)
	{
		super(BlockBehaviour.Properties.of()
				.strength(-1.0F, 3600000.0F)
				.noLootTable()
				.sound(SoundType.STONE));

		yieldToProvide = yieldStack;
		this.multiplier = multiplier;
	}

	@Override
	public ItemStack getYieldToProvide()
	{
		return yieldToProvide;
	}

	@Override
	public float getMultiplier()
	{
		return multiplier;
	}

	@Override
	public boolean canEntityDestroy(BlockState state, BlockGetter world, BlockPos pos, Entity entity)
	{
		return false;
	}
}
