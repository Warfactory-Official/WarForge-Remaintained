package com.flansmod.warforge.common.network;

import net.minecraft.network.FriendlyByteBuf;
import com.flansmod.warforge.platform.PayloadBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Base class for all packets in Flan's Mod. Every packet type is listed in {@link WFNetwork#PACKETS}.
 */
public abstract class PacketBase extends PayloadBase
{
	/**
	 * Encode the packet into a FriendlyByteBuf stream.
	 */
	public abstract void encodeInto(FriendlyByteBuf data);

	/**
	 * Decode the packet from a FriendlyByteBuf stream.
	 */
	public abstract void decodeInto(FriendlyByteBuf data);

	/**
	 * Handle the packet on server side, post-decoding
	 */
	public abstract void handleServerSide(ServerPlayer playerEntity);

	/**
	 * Handle the packet on client side, post-decoding
	 */
	public abstract void handleClientSide(Player clientPlayer);

	/**
	 * Util method for quickly writing strings
	 */
	public static void writeUTF(FriendlyByteBuf data, String s)
	{
		data.writeUtf(s);
	}

	/**
	 * Writes a string with the same cap the reader enforces, truncating instead of throwing so an
	 * over-long value can never blow up the encoder. The receiving side validates the content
	 * properly and reports a real error to the player.
	 */
	public static void writeUTF(FriendlyByteBuf data, String s, int maxLength)
	{
		String value = s == null ? "" : s;
		if (value.length() > maxLength)
		{
			value = value.substring(0, maxLength);
		}
		data.writeUtf(value, maxLength);
	}

	/**
	 * Util method for quickly reading strings
	 */
	public static String readUTF(FriendlyByteBuf data)
	{
		return data.readUtf();
	}

	public static String readUTF(FriendlyByteBuf data, int maxLength)
	{
		return data.readUtf(maxLength);
	}

	public static void writeUUID(FriendlyByteBuf data, UUID id)
	{
		data.writeUUID(id);
	}

	public static UUID readUUID(FriendlyByteBuf data)
	{
		return data.readUUID();
	}
}
