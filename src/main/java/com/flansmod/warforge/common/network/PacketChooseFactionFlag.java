package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionFlagSelectGuiFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PacketChooseFactionFlag extends PacketBase {
    public UUID factionId;
    public String flagId = "";

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUUID(data, factionId);
        writeUTF(data, flagId);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        factionId = readUUID(data);
        flagId = readUTF(data);
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        WarForgeMod.FACTIONS.requestChooseFactionFlag(player, factionId, flagId);
        FactionFlagSelectGuiFactory.INSTANCE.open(player, factionId);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
    }
}
