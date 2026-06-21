package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketRequestNamePlate extends PacketBase{
    public String name;
    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUTF(data, name);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        name = readUTF(data);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        WarForgeMod.FACTIONS.requestNamePlateCacheEntry(playerEntity, name);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {

    }
}
