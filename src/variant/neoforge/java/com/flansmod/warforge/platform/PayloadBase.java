package com.flansmod.warforge.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Loader base class of every WarForge packet. On NeoForge a packet is a CustomPacketPayload. */
public abstract class PayloadBase implements CustomPacketPayload {
    @Override
    public final Type<? extends CustomPacketPayload> type() {
        return NetworkPlatform.typeOf(getClass());
    }
}
