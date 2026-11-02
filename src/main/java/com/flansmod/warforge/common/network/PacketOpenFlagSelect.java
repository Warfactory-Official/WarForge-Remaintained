package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketOpenFlagSelect extends PacketBase {
    public UUID factionId = FactionIds.NONE;
    public String factionName = "";
    public int factionColor = 0xFFFFFF;
    public String currentFlagId = "";

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUUID(data, factionId);
        writeUTF(data, factionName);
        data.writeInt(factionColor);
        writeUTF(data, currentFlagId);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        factionId = readUUID(data);
        factionName = readUTF(data, PacketSecurity.MAX_NAME);
        factionColor = data.readInt();
        currentFlagId = readUTF(data, PacketSecurity.MAX_NAME);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
    }
}
