package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketDisbandFaction extends PacketBase
{

	// No data, you can only use this to disband your faction
	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
	}

	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		WarForgeMod.FACTIONS.requestDisbandFaction(playerEntity, Faction.nullUuid);
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
	}

}
