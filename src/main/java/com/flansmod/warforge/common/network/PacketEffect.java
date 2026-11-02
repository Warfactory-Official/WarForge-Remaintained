package com.flansmod.warforge.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PacketEffect extends PacketBase {
    public static final String TYPE_DISBAND = "disband";
    public static final String TYPE_UPGRADE = "upgrade";

    public double x, y, z;
    public String type = "";
    public String dataNBT = "";


    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeDouble(x);
        data.writeDouble(y);
        data.writeDouble(z);
        writeUTF(data, type);
        writeUTF(data, dataNBT);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        this.x = data.readDouble();
        this.y = data.readDouble();
        this.z = data.readDouble();
        this.type = readUTF(data);
        this.dataNBT = readUTF(data);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        PacketSecurity.violation(playerEntity, "effect packet received on server");
    }
}
