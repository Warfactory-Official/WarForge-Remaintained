package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketRequestUpgradeUI extends PacketRequestFactionInfo {

    @Override
    public void handleServerSide(ServerPlayer player) {
        Faction faction = null;
        if (!mFactionIDRequest.equals(Faction.nullUuid)) {
            faction = WarForgeMod.FACTIONS.getFaction(mFactionIDRequest);
        } else if (!mFactionNameRequest.isEmpty()) {
            faction = WarForgeMod.FACTIONS.getFaction(mFactionNameRequest);
        } else {
            WarForgeMod.LOGGER.error("Player " + player.getName().getString() + " made a request for faction info with no valid key");
        }

        if (faction != null) {
            UUID playerUUID = player.getUUID();
            PacketUpgradeUI packet = new PacketUpgradeUI();
            packet.mFactionID = faction.uuid;
            packet.mFactionName = faction.name;
            packet.color = faction.colour;
            packet.level = faction.citadelLevel;
            packet.outrankingOfficer = faction.isPlayerRoleInFaction(playerUUID, Faction.Role.OFFICER);
            WarForgeMod.NETWORK.sendTo(packet, player);
        } else {
            WarForgeMod.LOGGER.error("Could not find faction for info");
        }
    }
}
