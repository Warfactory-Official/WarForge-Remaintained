package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketRemoveClaim extends PacketBase
{
	public DimBlockPos pos;

	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		data.writeUtf(Keys.id(pos.dim).toString());
		data.writeInt(pos.getX());
		data.writeInt(pos.getY());
		data.writeInt(pos.getZ());
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
		int x = data.readInt();
		int y = data.readInt();
		int z = data.readInt();
		pos = new DimBlockPos(dim, x, y, z);
	}

	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		// Sent from the claim/siege-camp block GUI, so the player is always standing at the claim.
		// requestRemoveClaim then checks officer rank in the owning faction.
		if (!PacketSecurity.requireChunkInClaimRange(playerEntity, pos == null ? null : pos.toChunkPos(), "remove claim"))
			return;

		WarForgeMod.FACTIONS.requestRemoveClaim(playerEntity, pos);
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
	}

}
