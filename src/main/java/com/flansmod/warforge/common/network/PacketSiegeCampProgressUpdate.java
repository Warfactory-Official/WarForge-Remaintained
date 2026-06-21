package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PacketSiegeCampProgressUpdate extends PacketBase
{
	public SiegeCampProgressInfo info;

	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		// Attack
		data.writeUtf(info.attackingPos.dim.location().toString());
		data.writeInt(info.attackingPos.getX());
		data.writeInt(info.attackingPos.getY());
		data.writeInt(info.attackingPos.getZ());
		data.writeInt(info.attackingColour);
		writeUTF(data, info.attackingName);

		// Defend
		data.writeUtf(info.defendingPos.dim.location().toString());
		data.writeInt(info.defendingPos.getX());
		data.writeInt(info.defendingPos.getY());
		data.writeInt(info.defendingPos.getZ());
		data.writeInt(info.defendingColour);
		writeUTF(data, info.defendingName);
		data.writeInt(info.battleRadius);

		data.writeInt(info.progress);
		data.writeInt(info.mPreviousProgress);
		data.writeInt(info.completionPoint);

		//Common
		data.writeLong(info.timeProgress);
		data.writeLong(info.endTimestamp);
		data.writeBoolean(info.finished);
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		info = new SiegeCampProgressInfo();

		// Attacking
		ResourceKey<Level> attackingDim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.readUtf()));
		int x = data.readInt();
		int y = data.readInt();
		int z = data.readInt();
		info.attackingPos = new DimBlockPos(attackingDim, x, y, z);
		info.attackingColour = data.readInt();
		info.attackingName = readUTF(data);

		// Defending
		ResourceKey<Level> defendingDim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.readUtf()));
		x = data.readInt();
		y = data.readInt();
		z = data.readInt();
		info.defendingPos = new DimBlockPos(defendingDim, x, y, z);
		info.defendingColour = data.readInt();
		info.defendingName = readUTF(data);
		info.battleRadius = data.readInt();

		info.progress = data.readInt();
		info.mPreviousProgress = data.readInt();
		info.completionPoint = data.readInt();

		//Common
		info.timeProgress = data.readLong();
        info.endTimestamp = data.readLong();
		info.finished = data.readBoolean();
	}

	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		WarForgeMod.LOGGER.error("Received siege info on server");
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
		WarForgeMod.proxy.UpdateSiegeInfo(info);
	}
}
