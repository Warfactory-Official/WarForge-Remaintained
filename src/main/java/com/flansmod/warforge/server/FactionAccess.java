package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.compat.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

import static com.flansmod.warforge.server.ServerText.t;

/** Uniform op-bypass role checks with their denial message. */
final class FactionAccess {
    private FactionAccess() {
    }

    /** @return the player's faction, or null after telling them they have none */
    static Faction requireFaction(Player player) {
        Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            player.sendSystemMessage(t("not_in_faction"));
        }
        return faction;
    }

    static boolean requireRole(Player player, Faction faction, Role role) {
        if (WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUUID(), role)) {
            return true;
        }
        player.sendSystemMessage(denial(role));
        return false;
    }

    static boolean requireRole(CommandSourceStack source, Faction faction, Role role) {
        if (Perms.isGameMaster(source)
                || (source.getEntity() instanceof Player player && faction.isPlayerRoleInFaction(player.getUUID(), role))) {
            return true;
        }
        source.sendSystemMessage(denial(role));
        return false;
    }

    private static net.minecraft.network.chat.Component denial(Role role) {
        return t(role == Role.LEADER ? "role.leader_required" : "role.officer_required");
    }
}
