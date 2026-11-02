package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.server.level.ServerPlayer;

/** Runs the {@link Exposure} gate of a serverbound packet. */
final class PacketVetting {
    private PacketVetting() {}

    /** @return false after reporting a violation: the handler must not run */
    static boolean vet(ServerPlayer sender, ServerboundPacket packet, Exposure exposure) {
        String name = packet.getClass().getSimpleName();
        switch (exposure.kind()) {
            case PUBLIC, SELF -> {
                return true;
            }
            case OWN_FACTION -> {
                return requireFaction(sender, null, name);
            }
            case OWN_FACTION_ROLE -> {
                return requireFaction(sender, exposure, name);
            }
            case IN_RANGE -> {
                return PacketSecurity.requireChunkInRange(sender, packet.target(), exposure.radius().getAsInt(), name);
            }
        }
        throw new IllegalStateException(exposure.kind().toString());
    }

    private static boolean requireFaction(ServerPlayer sender, Exposure roleGate, String name) {
        if (WarForgeMod.isOp(sender)) {
            return true;
        }
        Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(sender.getUUID());
        if (faction == null) {
            PacketSecurity.violation(sender, name + " without a faction");
            return false;
        }
        if (roleGate != null && !faction.isPlayerRoleInFaction(sender.getUUID(), roleGate.minRole())) {
            PacketSecurity.violation(sender, name + " below role " + roleGate.minRole());
            return false;
        }
        return true;
    }
}
