package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketNamePlateChange;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Name plate (faction tag) broadcasts to nearby clients. */
public final class NamePlateSync {
    public static final int BROADCAST_RADIUS = 100;

    /** {@code factionName == null} removes the plate. */
    public void broadcast(Player player, String factionName, int colour) {
        PacketNamePlateChange packet = new PacketNamePlateChange();
        packet.name = player.getName().getString();
        if (factionName == null) {
            packet.isRemove = true;
        } else {
            packet.faction = factionName;
            packet.color = colour;
        }
        WarForgeMod.NETWORK.sendToAllAround(packet, player.getX(), player.getY(), player.getZ(),
                BROADCAST_RADIUS, player.level().dimension());
    }

    /** Re-broadcasts every online alive member's plate (rename, recolour). */
    public void broadcastFaction(Faction faction) {
        for (Player member : faction.getOnlinePlayers(Player::isAlive)) {
            broadcast(member, faction.getName(), faction.getColour());
        }
    }

    public void broadcastRemoval(Faction faction) {
        for (Player member : faction.getOnlinePlayers(p -> true)) {
            broadcast(member, null, 0);
        }
    }

    /** Only players the asker can currently see are looked up. */
    private static final double LOOKUP_RANGE_SQ = 256.0 * 256.0;

    /** Reply to a client lookup; players without a faction get an empty entry. */
    public void reply(ServerPlayer asker, String name) {
        ServerPlayer target = WarForgeMod.MC_SERVER.getPlayerList().getPlayerByName(name);
        if (target == null || target.level() != asker.level() || asker.distanceToSqr(target) > LOOKUP_RANGE_SQ) {
            return;
        }
        PacketNamePlateChange packet = new PacketNamePlateChange();
        packet.name = name;
        Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(target.getUUID());
        if (faction != null) {
            packet.faction = faction.getName();
            packet.color = faction.getColour();
        }
        WarForgeMod.NETWORK.sendTo(packet, asker);
    }
}
