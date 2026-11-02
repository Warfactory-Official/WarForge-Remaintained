package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketClientNotification;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.compat.Chat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

/** Toasts and chat notices to faction members. */
public final class FactionNotifier {
    public static final int INFO = PacketClientNotification.COLOR_INFO;
    public static final int SUCCESS = PacketClientNotification.COLOR_SUCCESS;
    public static final int WARNING = PacketClientNotification.COLOR_WARNING;
    public static final int DANGER = PacketClientNotification.COLOR_DANGER;

    public static final int DURATION_PRESENCE_MS = 4000;
    public static final int DURATION_DEFAULT_MS = 5000;
    public static final int DURATION_LONG_MS = 6000;
    public static final int DURATION_SIEGE_MS = 7000;
    public static final int DURATION_ALERT_MS = 8000;

    public void toPlayer(ServerPlayer player, String token, Component title, Component subtitle, int accentColor, int durationMs, UUID actorId) {
        if (player != null) {
            WarForgeMod.NETWORK.sendTo(new PacketClientNotification(token, title, subtitle, accentColor, durationMs, actorId), player);
        }
    }

    public void toFaction(Faction faction, String token, Component title, Component subtitle, int accentColor, int durationMs) {
        toFaction(faction, token, title, subtitle, accentColor, durationMs, null, List.of());
    }

    public void toFaction(Faction faction, String token, Component title, Component subtitle, int accentColor, int durationMs, UUID actorId) {
        toFaction(faction, token, title, subtitle, accentColor, durationMs, actorId, List.of());
    }

    public void toFaction(Faction faction, String token, Component title, Component subtitle, int accentColor, int durationMs,
                          UUID actorId, Collection<UUID> exclude) {
        if (faction == null) {
            return;
        }
        for (Player member : faction.getOnlinePlayers(p -> !exclude.contains(p.getUUID()))) {
            if (member instanceof ServerPlayer serverPlayer) {
                toPlayer(serverPlayer, token, title, subtitle, accentColor, durationMs, actorId);
            }
        }
    }

    public void siegeStarted(Faction attackers, Faction defenders, DimBlockPos defendingClaim) {
        String target = defendingClaim.toFancyString();
        toFaction(attackers, "siege_started_attackers_" + defendingClaim, t("toast.siege_started_attackers.title"),
                t("toast.siege_started_attackers.body", defenders.getName(), target), attackers.getColour(), DURATION_LONG_MS);
        toFaction(defenders, "siege_started_defenders_" + defendingClaim, t("toast.siege_started_defenders.title"),
                t("toast.siege_started_defenders.body", attackers.getName(), target), DANGER, DURATION_SIEGE_MS);
    }

    public void siegeOutcome(Faction attackers, Faction defenders, DimBlockPos defendingClaim, boolean attackersWon) {
        String target = defendingClaim.toFancyString();
        if (attackersWon) {
            toFaction(attackers, "siege_won_attackers_" + defendingClaim, t("toast.siege_won.title"),
                    t("toast.siege_won_attackers.body", defenders.getName(), target), SUCCESS, DURATION_SIEGE_MS);
            toFaction(defenders, "siege_lost_defenders_" + defendingClaim, t("toast.siege_lost.title"),
                    t("toast.siege_lost_defenders.body", attackers.getName(), target), DANGER, DURATION_SIEGE_MS);
        } else {
            toFaction(attackers, "siege_lost_attackers_" + defendingClaim, t("toast.siege_lost.title"),
                    t("toast.siege_lost_attackers.body", target), DANGER, DURATION_SIEGE_MS);
            toFaction(defenders, "siege_won_defenders_" + defendingClaim, t("toast.siege_won.title"),
                    t("toast.siege_won_defenders.body", target, attackers.getName()), SUCCESS, DURATION_SIEGE_MS);
        }
    }

    public void memberJoined(Faction faction, Player player) {
        toFaction(faction, "faction_member_joined_" + faction.getUuid() + "_" + player.getUUID(), t("toast.member_joined.title"),
                t("toast.member_joined.body", player.getName().getString(), faction.getName()), faction.getColour(), DURATION_DEFAULT_MS,
                player.getUUID(), List.of(player.getUUID()));
    }

    public void memberLeft(Faction faction, UUID playerId, String playerName, boolean wasKicked) {
        toFaction(faction, "faction_member_left_" + faction.getUuid() + "_" + playerName,
                t(wasKicked ? "toast.member_removed.title" : "toast.member_left.title"),
                t(wasKicked ? "toast.member_removed.body" : "toast.member_left.body", playerName, faction.getName()),
                wasKicked ? WARNING : DANGER, DURATION_DEFAULT_MS, playerId);
    }

    public void presence(Faction faction, UUID playerId, String playerName, boolean online) {
        toFaction(faction, "faction_presence_" + faction.getUuid() + "_" + playerName,
                t(online ? "toast.member_online.title" : "toast.member_offline.title"),
                t(online ? "toast.member_online.body" : "toast.member_offline.body", playerName),
                online ? INFO : WARNING, DURATION_PRESENCE_MS, playerId, List.of(playerId));
    }

    public void claimChanged(Faction faction, String token, Component title, Component subtitle, int accentColor, UUID actorId) {
        toFaction(faction, token, title, subtitle, accentColor, DURATION_DEFAULT_MS, actorId);
    }

    public void upgraded(Faction faction, UUID playerId, String playerName) {
        toFaction(faction, "faction_upgrade_" + faction.getUuid() + "_" + faction.getCitadelLevel(), t("toast.upgraded.title"),
                t("toast.upgraded.body", playerName, faction.getName(), faction.getCitadelLevel()),
                SUCCESS, DURATION_LONG_MS, playerId);
    }

    public void disbanded(Faction faction) {
        toFaction(faction, "faction_disbanded_" + faction.getUuid(), t("toast.disbanded.title"),
                t("toast.disbanded.body", faction.getName()), DANGER, DURATION_SIEGE_MS);
    }

    /** Chat line with a click-to-accept button. */
    public Component inviteMessage(Faction faction) {
        MutableComponent message = t("invite.received", faction.getName()).append(" ");
        MutableComponent clickPart = t("invite.click_to_join");
        clickPart.setStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withUnderlined(true)
                .withClickEvent(Chat.runCommand("/f accept " + faction.getName()))
                .withHoverEvent(Chat.showText(t("invite.hover", faction.getName()))));
        return message.append(clickPart);
    }
}
