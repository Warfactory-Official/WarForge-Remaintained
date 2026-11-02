package com.flansmod.warforge.common.network;

import net.minecraft.network.FriendlyByteBuf;
import com.flansmod.warforge.platform.PayloadBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Base class of all WarForge packets. Every packet type is listed in {@link WFNetwork#PACKETS}. */
public abstract class PacketBase extends PayloadBase
{
    public abstract void encodeInto(FriendlyByteBuf data);

    public abstract void decodeInto(FriendlyByteBuf data);

    public abstract void handleServerSide(ServerPlayer playerEntity);

    public void handleClientSide(Player clientPlayer)
    {
        ClientPacketRouter.dispatch(this, clientPlayer);
    }

    public static void writeUTF(FriendlyByteBuf data, String s)
    {
        data.writeUtf(s);
    }

    // truncates instead of throwing; receiver validates
    public static void writeUTF(FriendlyByteBuf data, String s, int maxLength)
    {
        String value = s == null ? "" : s;
        if (value.length() > maxLength)
        {
            value = value.substring(0, maxLength);
        }
        data.writeUtf(value, maxLength);
    }

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
