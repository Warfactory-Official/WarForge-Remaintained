package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityBasicClaim extends TileEntityYieldCollector implements IClaim
{
	public static final int NUM_SLOTS = NUM_BASE_SLOTS; // No additional slots here


	public TileEntityBasicClaim(BlockPos pos, BlockState state)
	{
		super(Content.TE_BASIC_CLAIM.get(), pos, state);
	}

	protected TileEntityBasicClaim(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	@Override
	public int getDefenceStrength() { return WarForgeConfig.CLAIM_STRENGTH_BASIC; }
	@Override
	public int getSupportStrength() { return WarForgeConfig.SUPPORT_STRENGTH_BASIC; }
	@Override
	public int getAttackStrength() { return 0; }
	@Override
	protected float getYieldMultiplier() { return 1.0f; }

}
