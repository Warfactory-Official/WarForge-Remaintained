package com.flansmod.warforge.common;

import com.flansmod.warforge.common.network.PacketClientNotification;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

/** Chat broadcasts and toasts to players. */
public final class Notifications {
    private Notifications() {}

    public static void messageAll(Component message) {
        if (WarForgeMod.MC_SERVER != null) {
            for (ServerPlayer player : WarForgeMod.MC_SERVER.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
            }
        }
    }

    public static void notifyPlayer(ServerPlayer player, String token, Component title, Component subtitle, int accentColor, int durationMs) {
        if (player != null) {
            WarForgeMod.NETWORK.sendTo(new PacketClientNotification(token, title, subtitle, accentColor, durationMs, null), player);
        }
    }

    public static void notifyFaction(Faction faction, String token, Component title, Component subtitle, int accentColor, int durationMs) {
        if (faction == null) {
            return;
        }
        for (Player player : faction.getOnlinePlayers(Objects::nonNull)) {
            if (player instanceof ServerPlayer serverPlayer) {
                notifyPlayer(serverPlayer, token, title, subtitle, accentColor, durationMs);
            }
        }
    }
}
