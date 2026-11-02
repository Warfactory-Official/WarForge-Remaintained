package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketRequestUpgradeUI extends PacketRequestFactionInfo {
    @Override
    public void handleServerSide(ServerPlayer player) {
        // serve only the player's own faction, not the id the client asked about
        Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            return;
        }
        if (!factionIdRequest.equals(FactionIds.NONE) && !factionIdRequest.equals(faction.getUuid())) {
            PacketSecurity.violation(player, "upgrade UI request for another faction (" + factionIdRequest + ")");
            return;
        }

        UUID playerUUID = player.getUUID();
        PacketUpgradeUI packet = new PacketUpgradeUI();
        packet.factionId = faction.getUuid();
        packet.factionName = faction.getName();
        packet.color = faction.getColour();
        packet.level = faction.getCitadelLevel();
        packet.outrankingOfficer = faction.isPlayerRoleInFaction(playerUUID, Role.OFFICER);
        WarForgeMod.NETWORK.sendTo(packet, player);
    }
}
