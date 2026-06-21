package com.flansmod.warforge.common;

import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CommonProxy
{
	// determines the bonus multiplier or reduction multiplier (reciprocal) for veins of
	// increasing or decreasing quality, respectively; not configurable for now, but would need sync packets if it were
	public static float YIELD_QUALITY_MULTIPLIER = 2;

	public void TickClient()
	{

	}

	public void TickServer()
	{

	}

	public BlockEntity GetTile(DimBlockPos pos)
	{
		Level level = WarForgeMod.MC_SERVER.getLevel(pos.dim);
		if (level != null)
			return level.getBlockEntity(pos.toRegularPos());

		WarForgeMod.LOGGER.error("GetTile failed; dimension {} is not loaded on the server", pos.dim.location());
		return null;
	}

	public void UpdateSiegeInfo(SiegeCampProgressInfo info)
	{
		// Do nothing, update on client
	}
}
