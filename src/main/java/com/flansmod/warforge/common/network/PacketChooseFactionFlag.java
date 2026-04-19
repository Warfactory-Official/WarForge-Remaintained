package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionFlagSelectGuiFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketChooseFactionFlag extends PacketBase {
    public UUID factionId;
    public String flagId = "";

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        writeUUID(data, factionId);
        writeUTF(data, flagId);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        factionId = readUUID(data);
        flagId = readUTF(data);
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        WarForgeMod.FACTIONS.requestChooseFactionFlag(playerEntity, factionId, flagId);
        FactionFlagSelectGuiFactory.INSTANCE.open(playerEntity, factionId);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
