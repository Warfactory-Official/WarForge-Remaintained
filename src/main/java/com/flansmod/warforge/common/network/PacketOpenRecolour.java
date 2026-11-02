package com.flansmod.warforge.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PacketOpenRecolour extends PacketBase {
    public int colour = 0xFFFFFF;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeInt(colour);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        colour = data.readInt();
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
    }
}
