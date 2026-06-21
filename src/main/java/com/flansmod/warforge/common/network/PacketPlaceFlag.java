package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PacketPlaceFlag extends PacketBase
{
	public DimBlockPos pos;

	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		writeUTF(data, pos.dim.location().toString());
		data.writeInt(pos.getX());
		data.writeInt(pos.getY());
		data.writeInt(pos.getZ());
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(readUTF(data)));
		int x = data.readInt();
		int y = data.readInt();
		int z = data.readInt();
		pos = new DimBlockPos(dim, x, y, z);
	}

	@Override
	public void handleServerSide(ServerPlayer player)
	{
		//WarForgeMod.FACTIONS.requestPlaceFlag(player, pos);
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
		 //noop
	}

}
