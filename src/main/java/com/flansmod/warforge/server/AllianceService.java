package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.TimeHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

/** Alliances, alliance requests and post-break truces. */
public final class AllianceService {
    private final FactionStorage hub;

    AllianceService(FactionStorage hub) {
        this.hub = hub;
    }

    /** Player-facing reason {@code attacker} may not siege {@code defender} for diplomatic reasons, or null. */
    Component getSiegeBlockReason(Faction attacker, Faction defender) {
        if (FactionIds.isNeutralZone(defender.getUuid())) {
            return t("alliance.siege_zone");
        }
        if (attacker.isAllyOf(defender.getUuid())) {
            return t("alliance.siege_ally", defender.getName());
        }
        if (attacker.isInTruceWith(defender.getUuid())) {
            return t("alliance.siege_truce", defender.getName(), TimeHelper.formatTime(attacker.getTruceRemainingMs(defender.getUuid())));
        }
        return null;
    }

    /** Disband: drops the faction from every other faction's alliance, request and truce sets. */
    void removeFactionEverywhere(UUID factionId) {
        for (Faction other : hub.registry.getAllFactions()) {
            other.removeAlly(factionId);
            other.removeAllianceRequest(factionId);
            other.removeTruce(factionId);
        }
    }

    /** Factions {@code faction} could invite: not itself, a zone, or an ally. */
    public List<Faction> getAlliableFactions(Faction faction) {
        ArrayList<Faction> result = new ArrayList<>();
        for (Faction other : hub.registry.getAllFactions()) {
            if (!other.getUuid().equals(faction.getUuid()) && !FactionIds.isNeutralZone(other.getUuid()) && !faction.isAllyOf(other.getUuid())) {
                result.add(other);
            }
        }
        return result;
    }

    private Faction requireManager(ServerPlayer player) {
        Faction faction = FactionAccess.requireFaction(player);
        return faction != null && FactionAccess.requireRole(player, faction, Role.OFFICER) ? faction : null;
    }

    private static boolean atAllyCap(Faction faction) {
        return WarForgeConfig.MAX_ALLIES >= 0 && faction.getAllies().size() >= WarForgeConfig.MAX_ALLIES;
    }

    public void requestInviteAlly(ServerPlayer player, UUID targetFactionId) {
        Faction faction = requireManager(player);
        if (faction == null) {
            return;
        }
        Faction target = hub.registry.getFaction(targetFactionId);
        if (target == null || FactionIds.isNeutralZone(targetFactionId)) {
            player.sendSystemMessage(t("alliance.target_gone"));
        } else if (target.getUuid().equals(faction.getUuid())) {
            player.sendSystemMessage(t("alliance.self"));
        } else if (faction.isAllyOf(target.getUuid())) {
            player.sendSystemMessage(t("alliance.already_allied", target.getName()));
        } else if (atAllyCap(faction)) {
            player.sendSystemMessage(t("alliance.cap"));
        } else if (faction.hasPendingAllianceRequestFrom(target.getUuid())) {
            // mutual consent reached
            form(faction, target);
        } else if (target.hasPendingAllianceRequestFrom(faction.getUuid())) {
            player.sendSystemMessage(t("alliance.already_requested", target.getName()));
        } else {
            target.addAllianceRequest(faction.getUuid());
            faction.messageAll(t("alliance.request_sent", target.getName()));
            target.messageAll(t("alliance.request_received", faction.getName()));
            FactionNotifier notifier = hub.notifier;
            notifier.toFaction(faction, "alliance_request_sent_" + target.getUuid(), t("toast.alliance_requested.title"),
                    t("toast.alliance_requested.body", target.getName()), FactionNotifier.INFO, FactionNotifier.DURATION_DEFAULT_MS);
            notifier.toFaction(target, "alliance_request_" + faction.getUuid(), t("toast.alliance_request.title"),
                    t("toast.alliance_request.body", faction.getName()), faction.getColour(), FactionNotifier.DURATION_ALERT_MS);
        }
    }

    public void requestAcceptAlliance(ServerPlayer player, UUID requesterFactionId) {
        Faction faction = requireManager(player);
        if (faction == null) {
            return;
        }
        if (!faction.removeAllianceRequest(requesterFactionId)) {
            player.sendSystemMessage(t("alliance.no_request"));
            return;
        }
        Faction requester = hub.registry.getFaction(requesterFactionId);
        if (requester == null) {
            player.sendSystemMessage(t("alliance.target_gone"));
        } else if (atAllyCap(faction) || atAllyCap(requester)) {
            player.sendSystemMessage(t("alliance.cap"));
        } else {
            form(faction, requester);
        }
    }

    private void form(Faction a, Faction b) {
        a.addAlly(b.getUuid());
        b.addAlly(a.getUuid());
        a.removeAllianceRequest(b.getUuid());
        b.removeAllianceRequest(a.getUuid());
        a.removeTruce(b.getUuid());
        b.removeTruce(a.getUuid());
        a.messageAll(t("alliance.formed", b.getName()));
        b.messageAll(t("alliance.formed", a.getName()));
        hub.notifier.toFaction(a, "alliance_formed_" + b.getUuid(), t("toast.alliance_formed.title"), t("toast.alliance_formed.body", b.getName()),
                FactionNotifier.SUCCESS, FactionNotifier.DURATION_LONG_MS);
        hub.notifier.toFaction(b, "alliance_formed_" + a.getUuid(), t("toast.alliance_formed.title"), t("toast.alliance_formed.body", a.getName()),
                FactionNotifier.SUCCESS, FactionNotifier.DURATION_LONG_MS);
    }

    public void requestDeclineAlliance(ServerPlayer player, UUID requesterFactionId) {
        Faction faction = requireManager(player);
        if (faction == null) {
            return;
        }
        if (!faction.removeAllianceRequest(requesterFactionId)) {
            player.sendSystemMessage(t("alliance.no_request"));
            return;
        }
        Faction requester = hub.registry.getFaction(requesterFactionId);
        if (requester == null) {
            faction.messageAll(t("alliance.declined"));
            return;
        }
        faction.messageAll(t("alliance.declined_from", requester.getName()));
        requester.messageAll(t("alliance.declined_by", faction.getName()));
        hub.notifier.toFaction(requester, "alliance_declined_" + faction.getUuid(), t("toast.alliance_declined.title"),
                t("toast.alliance_declined.body", faction.getName()), FactionNotifier.WARNING, FactionNotifier.DURATION_DEFAULT_MS);
    }

    public void requestBreakAlliance(ServerPlayer player, UUID allyFactionId) {
        Faction faction = requireManager(player);
        if (faction == null) {
            return;
        }
        if (!faction.isAllyOf(allyFactionId)) {
            player.sendSystemMessage(t("alliance.not_allied"));
            return;
        }
        Faction ally = hub.registry.getFaction(allyFactionId);
        faction.removeAlly(allyFactionId);
        if (ally != null) {
            ally.removeAlly(faction.getUuid());
        }
        long truceMs = (long) WarForgeConfig.ALLIANCE_TRUCE_DURATION_MINUTES * 60_000L;
        if (truceMs > 0) {
            long expiry = WarClock.truceClock() + truceMs;
            faction.setTruce(allyFactionId, expiry);
            if (ally != null) {
                ally.setTruce(faction.getUuid(), expiry);
            }
        }
        Component truce = truceMs > 0 ? t("alliance.truce_suffix", TimeHelper.formatTime(truceMs)) : Component.empty();
        String allyName = ally != null ? ally.getName() : "that faction";
        faction.messageAll(t("alliance.broken", allyName).append(truce));
        hub.notifier.toFaction(faction, "alliance_broken_" + allyFactionId, t("toast.alliance_broken.title"), t("toast.alliance_broken_self.body", allyName),
                FactionNotifier.WARNING, FactionNotifier.DURATION_LONG_MS);
        if (ally != null) {
            ally.messageAll(t("alliance.broken_by", faction.getName()).append(truce));
            hub.notifier.toFaction(ally, "alliance_broken_" + faction.getUuid(), t("toast.alliance_broken.title"),
                    t("toast.alliance_broken_other.body", faction.getName()), FactionNotifier.DANGER, FactionNotifier.DURATION_LONG_MS);
        }
    }

    public void requestToggleAllyInteraction(ServerPlayer player) {
        Faction faction = requireManager(player);
        if (faction == null) {
            return;
        }
        boolean allow = !faction.isAllowAllyInteraction();
        faction.setAllowAllyInteraction(allow);
        faction.messageAll(t(allow ? "alliance.interaction_on" : "alliance.interaction_off"));
        hub.notifier.toFaction(faction, "ally_interaction_" + faction.getUuid(), t("toast.ally_interaction.title"),
                t(allow ? "toast.ally_interaction_on.body" : "toast.ally_interaction_off.body"),
                FactionNotifier.INFO, FactionNotifier.DURATION_DEFAULT_MS);
    }

    /** Messages the faction and every transitively allied faction. */
    public void messageAlliance(Faction origin, Component chat) {
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        visited.add(origin.getUuid());
        queue.add(origin.getUuid());
        while (!queue.isEmpty()) {
            Faction faction = hub.registry.getFaction(queue.poll());
            if (faction == null) {
                continue;
            }
            faction.messageAll(chat);
            for (UUID allyId : faction.getAllies()) {
                if (hub.registry.getFaction(allyId) != null && visited.add(allyId)) {
                    queue.add(allyId);
                }
            }
        }
    }
}
