package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionStatsGuiFactory;
import com.flansmod.warforge.server.Faction.Role;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PacketFactionInfo extends PacketBase
{
	// Cheeky hack to make it available to the GUI
	public static FactionDisplayInfo latestInfo = null;

	public FactionDisplayInfo info;

	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		if(info != null)
		{
			data.writeBoolean(true);
			writeUUID(data, info.factionId);
			writeUTF(data, info.factionName);

			data.writeInt(info.notoriety);
			data.writeInt(info.wealth);
			data.writeInt(info.legacy);
			data.writeInt(info.lvl);

			data.writeInt(info.notorietyRank);
			data.writeInt(info.wealthRank);
			data.writeInt(info.legacyRank);
			data.writeInt(info.totalRank);

			data.writeInt(info.mNumClaims);

			writeUTF(data, info.mCitadelPos.dim.location().toString());
			data.writeInt(info.mCitadelPos.getX());
			data.writeInt(info.mCitadelPos.getY());
			data.writeInt(info.mCitadelPos.getZ());

			// Member list
			data.writeInt(info.members.size());
			for(int i = 0; i < info.members.size(); i++)
			{
				writeUUID(data, info.members.get(i).playerUuid);
				writeUTF(data, info.members.get(i).username);
				data.writeInt(info.members.get(i).role.ordinal());
			}
			writeUUID(data, info.mLeaderID);
		}
		else
		{
			data.writeBoolean(false);
		}

	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		boolean hasInfo = data.readBoolean();

		if(hasInfo)
		{
			info = new FactionDisplayInfo();

			info.factionId = readUUID(data);
			info.factionName = readUTF(data);

			info.notoriety = data.readInt();
			info.wealth = data.readInt();
			info.legacy = data.readInt();
			info.lvl = data.readInt();

			info.notorietyRank = data.readInt();
			info.wealthRank = data.readInt();
			info.legacyRank = data.readInt();
			info.totalRank = data.readInt();

			info.mNumClaims = data.readInt();

			ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(readUTF(data)));
			int x =	data.readInt();
			int y =	data.readInt();
			int z =	data.readInt();
			info.mCitadelPos = new DimBlockPos(dim, x, y, z);

			// Member list
			int count = data.readInt();
			for(int i = 0; i < count; i++)
			{
				PlayerDisplayInfo playerInfo = new PlayerDisplayInfo();
				playerInfo.playerUuid = readUUID(data);
				playerInfo.username = readUTF(data);
				playerInfo.role = Role.values()[data.readInt()];
				info.members.add(playerInfo);
			}
			info.mLeaderID = readUUID(data);
		}
		else
			info = null;
	}

	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		WarForgeMod.LOGGER.error("Received FactionInfo on server");
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
		latestInfo = info;
		if(info != null)
		{
			FactionStatsGuiFactory.INSTANCE.openClient(info.factionId);
		}
	}

}
