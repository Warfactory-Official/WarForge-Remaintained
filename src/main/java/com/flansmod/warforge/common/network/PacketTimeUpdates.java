package com.flansmod.warforge.common.network;

import com.flansmod.warforge.client.ClientTickHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketTimeUpdates extends PacketBase
{
	public long msTimeOfNextSiegeDay = 0L;
	public long msTimeOfNextYieldDay = 0L;

	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		data.writeLong(msTimeOfNextSiegeDay);
		data.writeLong(msTimeOfNextYieldDay);
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		msTimeOfNextSiegeDay = data.readLong();
		msTimeOfNextYieldDay = data.readLong();
	}

	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{

	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
		ClientTickHandler.nextSiegeDayMs = msTimeOfNextSiegeDay;
		ClientTickHandler.nextYieldDayMs = msTimeOfNextYieldDay;
	}

}
