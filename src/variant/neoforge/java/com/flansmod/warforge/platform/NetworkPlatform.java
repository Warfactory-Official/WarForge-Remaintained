package com.flansmod.warforge.platform;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.network.PacketBase;
import com.flansmod.warforge.common.network.WFNetwork;
import com.flansmod.warforge.compat.Ids;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;

/** Registers WFNetwork.PACKETS as NeoForge payloads and sends them. */
public final class NetworkPlatform {
    private NetworkPlatform() {}

    private static final Map<Class<?>, CustomPacketPayload.Type<?>> TYPES = new HashMap<>();

    public static void register(IEventBus modBus) {
        for (WFNetwork.Entry<?> entry : WFNetwork.PACKETS) {
            TYPES.put(entry.type(), new CustomPacketPayload.Type<>(Ids.of(Tags.MODID, entry.id())));
        }
        modBus.addListener(NetworkPlatform::onRegisterPayloads);
    }

    /** The payload type of a packet class (PacketBase.type()). */
    static CustomPacketPayload.Type<?> typeOf(Class<?> packetClass) {
        CustomPacketPayload.Type<?> type = TYPES.get(packetClass);
        if (type == null) {
            throw new IllegalArgumentException(packetClass.getName() + " is not registered in WFNetwork.PACKETS");
        }
        return type;
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(WFNetwork.VERSION);
        for (WFNetwork.Entry<?> entry : WFNetwork.PACKETS) {
            register(registrar, entry);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends PacketBase> void register(PayloadRegistrar registrar, WFNetwork.Entry<T> entry) {
        CustomPacketPayload.Type<T> type = (CustomPacketPayload.Type<T>) typeOf(entry.type());
        StreamCodec<RegistryFriendlyByteBuf, T> codec = StreamCodec.of(
                (buf, packet) -> packet.encodeInto(buf),
                buf -> {
                    T packet = entry.factory().get();
                    packet.decodeInto(buf);
                    return packet;
                });
        IPayloadHandler<T> handler = (payload, context) -> handle(payload, context, entry);
        switch (entry.direction()) {
            case TO_SERVER -> registrar.playToServer(type, codec, handler);
            case TO_CLIENT -> registrar.playToClient(type, codec, handler);
            case BOTH -> PayloadRegistration.bidirectional(registrar, type, codec, handler);
        }
    }

    private static void handle(PacketBase payload, IPayloadContext context, WFNetwork.Entry<?> entry) {
        context.enqueueWork(() -> {
            boolean serverbound = context.flow() == PacketFlow.SERVERBOUND;
            ServerPlayer sender = serverbound && context.player() instanceof ServerPlayer sp ? sp : null;
            WFNetwork.handle(payload, serverbound, sender, serverbound ? null : context.player(), entry.bucket());
        });
    }

    public static void sendToAll(PacketBase packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    public static void sendTo(PacketBase packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToDimension(PacketBase packet, ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, packet);
    }

    public static void sendToAllAround(PacketBase packet, ServerLevel level, double x, double y, double z, double range) {
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, packet);
    }

    public static void sendToServer(PacketBase packet) {
        ServerBound.send(packet);
    }
}
