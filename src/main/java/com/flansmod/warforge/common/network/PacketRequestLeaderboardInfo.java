package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Leaderboard.FactionStat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketRequestLeaderboardInfo extends PacketBase
{
	public FactionStat stat = FactionStat.TOTAL;
	public int firstIndex = 0;

	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		data.writeInt(firstIndex);
		data.writeInt(stat.ordinal());
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		firstIndex = data.readInt();
		stat = FactionStat.values()[data.readInt()];
	}

	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		PacketLeaderboardInfo packet = new PacketLeaderboardInfo();
		packet.info = WarForgeMod.LEADERBOARD.CreateInfo(firstIndex, stat, playerEntity.getUUID());
		WarForgeMod.NETWORK.sendTo(packet, playerEntity);
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
		WarForgeMod.LOGGER.error("Received LeaderboardInfo request on client");
	}

}
