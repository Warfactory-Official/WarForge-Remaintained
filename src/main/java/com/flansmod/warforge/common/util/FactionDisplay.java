package com.flansmod.warforge.common.util;

import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

public final class FactionDisplay {
    private FactionDisplay() {
    }

    public static MutableComponent factionPrefix(Faction faction) {
        if (faction == null || faction.getUuid().equals(FactionIds.NONE) || faction.getName() == null || faction.getName().isEmpty()) {
            return null;
        }
        return Component.literal("[" + faction.getName() + "] ")
                .withStyle(style -> style.withColor(TextColor.fromRgb(faction.getColour())));
    }

    public static Component tabName(Faction faction, String playerName) {
        MutableComponent prefix = factionPrefix(faction);
        if (prefix == null) {
            return null;
        }
        return Component.literal("").append(prefix).append(Component.literal(playerName));
    }

    public static void refreshTabName(UUID playerId) {
        // tab prefix disabled => push nothing, reveal no association
        if (!WarForgeConfig.FACTION_PREFIX_IN_TABLIST) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || playerId == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.refreshTabListName();
        }
    }

    public static void refreshFactionTabNames(Faction faction) {
        if (faction == null) {
            return;
        }
        for (UUID memberId : faction.getMemberIds()) {
            refreshTabName(memberId);
        }
    }
}
