package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PacketRequestUpgrade extends ServerboundPacket {
    public UUID factionID = FactionIds.NONE;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUUID(data, factionID);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        factionID = readUUID(data);
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        WarForgeMod.FACTIONS.upgrades.requestLevelUp(player, factionID);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {

    }
}
