package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityReinforcedClaim extends TileEntityBasicClaim
{
	public TileEntityReinforcedClaim(BlockPos pos, BlockState state)
	{
		super(Content.TE_REINFORCED_CLAIM.get(), pos, state);
	}

	@Override
	public int getDefenceStrength() { return WarForgeConfig.CLAIM_STRENGTH_REINFORCED; }
	@Override
	public int getSupportStrength() { return WarForgeConfig.SUPPORT_STRENGTH_REINFORCED; }
}
