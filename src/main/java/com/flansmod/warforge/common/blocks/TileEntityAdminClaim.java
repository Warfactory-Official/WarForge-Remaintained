package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityAdminClaim extends TileEntityClaim
{
	public TileEntityAdminClaim(BlockPos pos, BlockState state)
	{
		super(Content.TE_ADMIN_CLAIM.get(), pos, state);
	}

	// IClaim
	@Override
	public boolean canBeSieged() { return false; }
	// ------------
	@Override
	public int getAttackStrength() { return 0; }
	@Override
	public int getDefenceStrength() { return 0; }
	@Override
	public int getSupportStrength() { return 0; }
}
