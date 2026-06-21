package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PacketMoveCitadel extends PacketBase
{
	public DimBlockPos pos = DimBlockPos.ZERO;

	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		data.writeUtf(pos.dim.location().toString());
		data.writeInt(pos.getX());
		data.writeInt(pos.getY());
		data.writeInt(pos.getZ());
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.readUtf()));
		pos = new DimBlockPos(dim, data.readInt(), data.readInt(), data.readInt());
	}

	@Override
	public void handleServerSide(ServerPlayer player)
	{
		WarForgeMod.FACTIONS.requestMoveCitadel(player, pos);
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
		 //noop
	}

}
