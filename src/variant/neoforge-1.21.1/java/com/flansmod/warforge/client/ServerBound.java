package com.flansmod.warforge.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ServerBound {
    private ServerBound() {}

    public static void send(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
