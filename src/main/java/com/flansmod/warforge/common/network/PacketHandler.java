package com.flansmod.warforge.common.network;

import com.flansmod.warforge.client.ClientPacketHandler;

import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.platform.NetworkPlatform;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.flansmod.warforge.platform.Platform;

/**
 * Flan's Mod packet handler class. Directs packet data to packet classes.
 *
 * @author Flan
 */
public class PacketHandler
{
	/**
	 * Packet types are listed in WFNetwork.PACKETS and registered by platform.NetworkPlatform.
	 */
	public void initialise()
	{
	}

	public void postInitialise()
	{
	}

	public void handleClientPackets()
	{
	}

	public void handleServerPackets()
	{
	}

	/**
	 * Send a packet to all players
	 */
	public void sendToAll(PacketBase packet)
	{
		NetworkPlatform.sendToAll(packet);
	}

	/**
	 * Send a packet to a player
	 */
	public void sendTo(PacketBase packet, ServerPlayer player)
	{
		if (player == null) return;
		NetworkPlatform.sendTo(packet, player);
	}

	/**
	 * Send a packet to all in a dimension
	 */
	public void sendToDimension(PacketBase packet, ResourceKey<Level> dimension)
	{
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		ServerLevel level = server.getLevel(dimension);
		if (level != null) NetworkPlatform.sendToDimension(packet, level);
	}

	/**
	 * Send a packet to all around a point
	 */
	public void sendToAllAround(PacketBase packet, double x, double y, double z, float range, ResourceKey<Level> dimension)
	{
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		ServerLevel level = server.getLevel(dimension);
		if (level != null) NetworkPlatform.sendToAllAround(packet, level, x, y, z, range);
	}

	/**
	 * Send a packet to the server
	 */
	public void sendToServer(PacketBase packet)
	{
		NetworkPlatform.sendToServer(packet);
	}

	//Vanilla packets follow

	/**
	 * Send a packet to all players
	 */
	public void sendToAll(Packet<?> packet)
	{
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) server.getPlayerList().broadcastAll(packet);
	}

	/**
	 * Send a packet to a player
	 */
	public void sendTo(Packet<?> packet, ServerPlayer player)
	{
		if (player == null) return;
		player.connection.send(packet);
	}

	/**
	 * Send a packet to the server
	 */
	public void sendToServer(Packet<?> packet)
	{
		if (Platform.isClient())
			ClientPacketHandler.sendToServer(packet);
	}
}
