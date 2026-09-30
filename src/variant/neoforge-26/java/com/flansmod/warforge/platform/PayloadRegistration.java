package com.flansmod.warforge.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Two-way payload registration. NeoForge 26's three-argument playBidirectional only registers the server handler
 * (the client would refuse to start: "clientbound payloads are missing client-side handlers"), so the same handler
 * is also given for the client side, as 1.21.1 did.
 */
final class PayloadRegistration {
    private PayloadRegistration() {}

    static <T extends CustomPacketPayload> void bidirectional(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type,
                                                            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                            IPayloadHandler<T> handler) {
        registrar.playBidirectional(type, codec, handler, handler);
    }
}
