package com.flansmod.warforge.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Two-way payload registration. On 1.21.1 one handler serves both sides; NeoForge 26 takes a separate client one. */
final class PayloadRegistration {
    private PayloadRegistration() {}

    static <T extends CustomPacketPayload> void bidirectional(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type,
                                                            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                            IPayloadHandler<T> handler) {
        registrar.playBidirectional(type, codec, handler);
    }
}
