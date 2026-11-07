package com.flansmod.warforge.common.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.platform.NetworkPlatform;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class PacketHandler
{
    public void sendToAll(PacketBase packet)
    {
        if (WarForgeMod.isReplayServer()) return;
        NetworkPlatform.sendToAll(packet);
    }

    public void sendTo(PacketBase packet, ServerPlayer player)
    {
        if (player == null || WarForgeMod.isReplayServer()) return;
        NetworkPlatform.sendTo(packet, player);
    }

    public void sendToDimension(PacketBase packet, ResourceKey<Level> dimension)
    {
        if (WarForgeMod.isReplayServer()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel level = server.getLevel(dimension);
        if (level != null) NetworkPlatform.sendToDimension(packet, level);
    }

    public void sendToAllAround(PacketBase packet, double x, double y, double z, float range, ResourceKey<Level> dimension)
    {
        if (WarForgeMod.isReplayServer()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel level = server.getLevel(dimension);
        if (level != null) NetworkPlatform.sendToAllAround(packet, level, x, y, z, range);
    }

    public void sendToServer(PacketBase packet)
    {
        if (WarForgeMod.isReplayServer()) return;
        NetworkPlatform.sendToServer(packet);
    }

    public void sendToAll(Packet<?> packet)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.getPlayerList().broadcastAll(packet);
    }

    public void sendTo(Packet<?> packet, ServerPlayer player)
    {
        if (player == null) return;
        player.connection.send(packet);
    }
}
