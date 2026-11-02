package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketDisbandFaction extends ServerboundPacket
{
    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity)
    {
        WarForgeMod.FACTIONS.lifecycle.requestDisband(playerEntity, FactionIds.NONE);
    }

    @Override
    public void handleClientSide(Player clientPlayer)
    {
    }

}
