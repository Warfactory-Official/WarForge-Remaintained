package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Insurance menu view; stash contents travel as menu slots. */
public class PacketInsuranceData extends PacketBase {
    /** Client: view of the insurance menu opening next. */
    public static PacketInsuranceData latest = null;

    public boolean hasFaction = false;
    public UUID factionId = FactionIds.NONE;
    public String factionName = "";
    public int factionColor = 0xFFFFFF;
    public boolean canDeposit = false;
    public boolean canVoid = false;
    public int slotCount = 0;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeBoolean(hasFaction);
        writeUUID(data, factionId);
        writeUTF(data, factionName);
        data.writeInt(factionColor);
        data.writeBoolean(canDeposit);
        data.writeBoolean(canVoid);
        data.writeVarInt(slotCount);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        hasFaction = data.readBoolean();
        factionId = readUUID(data);
        factionName = readUTF(data);
        factionColor = data.readInt();
        canDeposit = data.readBoolean();
        canVoid = data.readBoolean();
        slotCount = data.readVarInt();
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        PacketSecurity.violation(player, "Received PacketInsuranceData on server side");
    }
}
