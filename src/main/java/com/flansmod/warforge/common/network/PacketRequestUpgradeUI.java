package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketRequestUpgradeUI extends PacketRequestFactionInfo {

    @Override
    public void handleServerSide(ServerPlayer player) {
        // The upgrade panel is only meaningful for your own faction, and it reports whether you
        // outrank an officer there - so serve it strictly for the faction the player belongs to
        // rather than for whatever id the client asked about.
        Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            return;
        }
        if (!mFactionIDRequest.equals(Faction.nullUuid) && !mFactionIDRequest.equals(faction.uuid)) {
            PacketSecurity.violation(player, "upgrade UI request for another faction (" + mFactionIDRequest + ")");
            return;
        }

        UUID playerUUID = player.getUUID();
        PacketUpgradeUI packet = new PacketUpgradeUI();
        packet.mFactionID = faction.uuid;
        packet.mFactionName = faction.name;
        packet.color = faction.colour;
        packet.level = faction.citadelLevel;
        packet.outrankingOfficer = faction.isPlayerRoleInFaction(playerUUID, Faction.Role.OFFICER);
        WarForgeMod.NETWORK.sendTo(packet, player);
    }
}
