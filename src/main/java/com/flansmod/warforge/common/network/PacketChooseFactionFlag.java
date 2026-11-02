package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PacketChooseFactionFlag extends ServerboundPacket {
    public UUID factionId;
    public String flagId = "";

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUUID(data, factionId);
        writeUTF(data, flagId, PacketSecurity.MAX_FLAG_ID);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        factionId = readUUID(data);
        flagId = readUTF(data, PacketSecurity.MAX_FLAG_ID);
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        // faction id is the client's: pin to the player's own faction
        Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
        if (!WarForgeMod.isOp(player) && (faction == null || !faction.getUuid().equals(factionId))) {
            PacketSecurity.violation(player, "flag selection for a faction they are not in (" + factionId + ")");
            return;
        }

        WarForgeMod.FACTIONS.lifecycle.requestChooseFlag(player, factionId, flagId);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
    }
}
