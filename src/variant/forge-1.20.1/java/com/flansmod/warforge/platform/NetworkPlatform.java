package com.flansmod.warforge.platform;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.network.PacketBase;
import com.flansmod.warforge.common.network.WFNetwork;
import com.flansmod.warforge.compat.Ids;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class NetworkPlatform {
    private NetworkPlatform() {}

    private static SimpleChannel channel;

    public static void register(IEventBus modBus) {
        channel = NetworkRegistry.newSimpleChannel(
                Ids.of(Tags.MODID, "main"), () -> WFNetwork.VERSION, WFNetwork.VERSION::equals, WFNetwork.VERSION::equals);
        int index = 0;
        for (WFNetwork.Entry<?> entry : WFNetwork.PACKETS) {
            register(index++, entry);
        }
    }

    private static <T extends PacketBase> void register(int index, WFNetwork.Entry<T> entry) {
        SimpleChannel.MessageBuilder<T> builder = switch (entry.direction()) {
            case TO_SERVER -> channel.messageBuilder(entry.type(), index, NetworkDirection.PLAY_TO_SERVER);
            case TO_CLIENT -> channel.messageBuilder(entry.type(), index, NetworkDirection.PLAY_TO_CLIENT);
            case BOTH -> channel.messageBuilder(entry.type(), index);
        };
        builder.encoder(PacketBase::encodeInto)
                .decoder(buf -> {
                    T packet = entry.factory().get();
                    packet.decodeInto(buf);
                    return packet;
                })
                .consumerMainThread((packet, context) -> handle(packet, context, entry))
                .add();
    }

    private static void handle(PacketBase packet, Supplier<NetworkEvent.Context> contextSupplier, WFNetwork.Entry<?> entry) {
        NetworkEvent.Context context = contextSupplier.get();
        boolean serverbound = context.getDirection().getReceptionSide().isServer();
        ServerPlayer sender = serverbound ? context.getSender() : null;
        WFNetwork.handle(packet, serverbound, sender, serverbound ? null : ClientAccess.player(), entry.bucket());
        context.setPacketHandled(true);
    }

    public static void sendToAll(PacketBase packet) {
        channel.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendTo(PacketBase packet, ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToDimension(PacketBase packet, ServerLevel level) {
        channel.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }

    public static void sendToAllAround(PacketBase packet, ServerLevel level, double x, double y, double z, double range) {
        channel.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(x, y, z, range, level.dimension())), packet);
    }

    public static void sendToServer(PacketBase packet) {
        channel.sendToServer(packet);
    }

    private static final class ClientAccess {
        static net.minecraft.world.entity.player.Player player() {
            return net.minecraft.client.Minecraft.getInstance().player;
        }
    }
}
