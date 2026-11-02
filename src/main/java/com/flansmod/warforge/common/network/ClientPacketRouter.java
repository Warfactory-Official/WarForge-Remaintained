package com.flansmod.warforge.common.network;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** Client handlers of clientbound packets, registered by client init so packet classes hold no client code. */
public final class ClientPacketRouter {
    private static final Map<Class<?>, BiConsumer<PacketBase, Player>> HANDLERS = new HashMap<>();

    private ClientPacketRouter() {}

    public static <T extends PacketBase> void register(Class<T> type, BiConsumer<T, Player> handler) {
        HANDLERS.put(type, (packet, player) -> handler.accept(type.cast(packet), player));
    }

    static void dispatch(PacketBase packet, Player player) {
        BiConsumer<PacketBase, Player> handler = HANDLERS.get(packet.getClass());
        if (handler == null) {
            throw new IllegalStateException("No client handler registered for " + packet.getClass().getSimpleName());
        }
        handler.accept(packet, player);
    }
}
