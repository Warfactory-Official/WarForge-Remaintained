package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketSetFactionColour extends PacketBase
{
	public int mColour = 0xffffff;

	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		data.writeInt(mColour);
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		mColour = data.readInt();
	}

	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		WarForgeMod.FACTIONS.requestSetFactionColour(playerEntity, mColour);
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{

	}

}
