package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketSetFactionColour extends ServerboundPacket
{
    public int colour = 0xffffff;

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeInt(colour);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        colour = data.readInt();
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity)
    {
        WarForgeMod.FACTIONS.lifecycle.requestSetColour(playerEntity, colour & 0xffffff);
    }

    @Override
    public void handleClientSide(Player clientPlayer)
    {

    }

}
