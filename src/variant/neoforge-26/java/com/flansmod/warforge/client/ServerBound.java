package com.flansmod.warforge.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ServerBound {
    private ServerBound() {}

    public static void send(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
