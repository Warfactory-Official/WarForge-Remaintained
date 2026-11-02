package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketUpgradeUI extends PacketBase {
    public static PacketUpgradeUI latest = null;

    public UUID factionId = FactionIds.NONE;
    public String factionName = "";
    public int level = 0;
    public int color = 0xffff;
    public boolean outrankingOfficer = false;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUUID(data, factionId);
        writeUTF(data, factionName);
        data.writeInt(level);
        data.writeInt(color);
        data.writeBoolean(outrankingOfficer);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        factionId = readUUID(data);
        factionName = readUTF(data);
        level = data.readInt();
        color = data.readInt();
        outrankingOfficer = data.readBoolean();
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        PacketSecurity.violation(player, "Received a Upgrade UI packet on serverside");
    }
}
