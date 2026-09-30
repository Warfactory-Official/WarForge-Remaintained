package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.flansmod.warforge.compat.Keys;

public class PacketCreateFaction extends PacketBase
{
	public DimBlockPos mCitadelPos;
	public String mFactionName = "";
	public int mColour = 0xffffff;


	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		writeUTF(data, Keys.id(mCitadelPos.dim).toString());
		data.writeInt(mCitadelPos.getX());
		data.writeInt(mCitadelPos.getY());
		data.writeInt(mCitadelPos.getZ());
		data.writeInt(mColour);
		writeUTF(data, mFactionName, PacketSecurity.MAX_NAME);
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
		int x = data.readInt();
		int y = data.readInt();
		int z = data.readInt();
		mCitadelPos = new DimBlockPos(dim, x, y, z);
		mColour = data.readInt();
		mFactionName = readUTF(data, PacketSecurity.MAX_NAME);
	}

	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		// The create-faction GUI only opens when the player is standing at the citadel they placed,
		// so require the same here rather than trusting a position the client picked.
		if(!PacketSecurity.requireReach(playerEntity, mCitadelPos, "create faction"))
			return;

		TileEntityCitadel citadel = PacketSecurity.blockEntityAt(playerEntity, mCitadelPos, TileEntityCitadel.class, "create faction");
		if(citadel == null)
			return;

		// requestCreateFaction owns the rest: citadel unowned, chunk unclaimed, caller is the placer.
		WarForgeMod.FACTIONS.requestCreateFaction(citadel, playerEntity, mFactionName, mColour & 0xffffff);
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
		WarForgeMod.LOGGER.error("Recieved create faction message on client");
	}

}
