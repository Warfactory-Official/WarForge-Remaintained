package com.flansmod.warforge.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PacketNamePlateChange extends PacketBase {
    public boolean isRemove = false;
    public String faction = "";
    public String name = "";
    public int color;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeBoolean(isRemove);
        writeUTF(data, faction);
        writeUTF(data, name);
        data.writeInt(color);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        isRemove = data.readBoolean();
        faction = readUTF(data);
        name = readUTF(data);
        color = data.readInt();

    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {

    }
}
