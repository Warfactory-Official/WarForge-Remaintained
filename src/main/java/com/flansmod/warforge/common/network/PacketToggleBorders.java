package com.flansmod.warforge.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Server to client: flip the claim border overlay (/f borders). */
public class PacketToggleBorders extends PacketBase {
    @Override
    public void encodeInto(FriendlyByteBuf data) {
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
    }
}
