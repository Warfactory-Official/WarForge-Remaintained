package com.flansmod.warforge.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Client-to-server send. NeoForge 26 moved it to ClientPacketDistributor. */
final class ServerBound {
    private ServerBound() {}

    static void send(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
