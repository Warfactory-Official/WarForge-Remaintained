package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.Role;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.faction.Role;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketFactionInfo extends PacketBase
{
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
            data.writeInt(info.colour);
            writeUTF(data, info.flagId);

            data.writeInt(info.notoriety);
            data.writeInt(info.wealth);
            data.writeInt(info.legacy);
            data.writeInt(info.lvl);

            data.writeInt(info.notorietyRank);
            data.writeInt(info.wealthRank);
            data.writeInt(info.legacyRank);
            data.writeInt(info.totalRank);

            data.writeInt(info.numClaims);

            writeUTF(data, Keys.id(info.citadelPos.dim).toString());
            data.writeInt(info.citadelPos.getX());
            data.writeInt(info.citadelPos.getY());
            data.writeInt(info.citadelPos.getZ());

            data.writeInt(info.members.size());
            for(int i = 0; i < info.members.size(); i++)
            {
                writeUUID(data, info.members.get(i).playerUuid);
                writeUTF(data, info.members.get(i).username);
                data.writeInt(info.members.get(i).role.ordinal());
            }
            writeUUID(data, info.leaderId);
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
            info.factionName = readUTF(data, PacketSecurity.MAX_NAME);
            info.colour = data.readInt();
            info.flagId = readUTF(data, PacketSecurity.MAX_NAME);

            info.notoriety = data.readInt();
            info.wealth = data.readInt();
            info.legacy = data.readInt();
            info.lvl = data.readInt();

            info.notorietyRank = data.readInt();
            info.wealthRank = data.readInt();
            info.legacyRank = data.readInt();
            info.totalRank = data.readInt();

            info.numClaims = data.readInt();

            ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
            int x =	data.readInt();
            int y =	data.readInt();
            int z =	data.readInt();
            info.citadelPos = new DimBlockPos(dim, x, y, z);

            int count = PacketSecurity.readCount(data, 4096);
            for(int i = 0; i < count; i++)
            {
                PlayerDisplayInfo playerInfo = new PlayerDisplayInfo();
                playerInfo.playerUuid = readUUID(data);
                playerInfo.username = readUTF(data, PacketSecurity.MAX_NAME);
                playerInfo.role = PacketSecurity.readEnumInt(data, Role.values(), Role.MEMBER);
                info.members.add(playerInfo);
            }
            info.leaderId = readUUID(data);
        }
        else
            info = null;
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity)
    {
        PacketSecurity.violation(playerEntity, "Received FactionInfo on server");
    }

}
