package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.FactionDisplay;
import com.flansmod.warforge.common.util.TimeHelper;
import com.flansmod.warforge.compat.Nbt;
import com.flansmod.warforge.compat.PlayerRef;
import com.flansmod.warforge.compat.Players;
import com.flansmod.warforge.compat.Profiles;
import com.flansmod.warforge.compat.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.flansmod.warforge.server.ServerText.t;

/** Joining, leaving, invites, rank changes, rejoin cooldowns and login/logout presence. */
public final class MembershipService {
    private final FactionStorage hub;
    private final HashMap<UUID, Long> rejoinCooldowns = new HashMap<>();

    MembershipService(FactionStorage hub) {
        this.hub = hub;
    }

    void join(Faction faction, UUID playerId) {
        faction.addMember(playerId);
        hub.registry.indexMember(playerId, faction);
        clearInvitesToPlayer(playerId);
        faction.messageAll(t("member.joined", playerName(playerId), faction.getName()));
        FactionDisplay.refreshTabName(playerId);
        FactionTeams.sync(faction);
    }

    void leave(Faction faction, UUID playerId) {
        faction.removeMember(playerId);
        hub.registry.unindexMember(playerId);
        FactionDisplay.refreshTabName(playerId);
        FactionTeams.sync(faction);
    }

    /** Sets the leader (previous leader becomes officer) and announces it. */
    boolean setLeader(Faction faction, UUID playerId) {
        if (!faction.setLeader(playerId)) {
            return false;
        }
        faction.messageAll(t("member.made_leader", playerName(playerId), faction.getName()));
        FactionTeams.sync(faction);
        return true;
    }

    private static String playerName(UUID playerId) {
        PlayerRef profile = Profiles.get(WarForgeMod.MC_SERVER, playerId).orElse(null);
        return profile != null ? profile.getName() : playerId.toString();
    }

    public ArrayList<Faction> getFactionsWithOpenInvitesTo(UUID playerID) {
        ArrayList<Faction> factions = new ArrayList<>();
        for (Faction faction : hub.registry.getAllFactions()) {
            if (faction.isInvitingPlayer(playerID)) {
                factions.add(faction);
            }
        }
        factions.sort(Comparator.comparing(Faction::getName, String.CASE_INSENSITIVE_ORDER));
        return factions;
    }

    public void clearInvitesToPlayer(UUID playerID) {
        for (Faction faction : hub.registry.getAllFactions()) {
            faction.removeInvite(playerID);
        }
    }

    public boolean requestInvitePlayerToMyFaction(ServerPlayer officer, UUID invitee) {
        Faction faction = hub.registry.getFactionOfPlayer(officer.getUUID());
        return faction != null && requestInvite(Players.commandSource(officer), faction.getUuid(), invitee);
    }

    public boolean requestInvite(CommandSourceStack officer, UUID factionID, UUID invitee) {
        Faction faction = hub.registry.getFaction(factionID);
        if (faction == null) {
            officer.sendSystemMessage(t("faction.not_found"));
            return false;
        }
        if (!FactionAccess.requireRole(officer, faction, Role.OFFICER)) {
            return false;
        }
        if (hub.registry.getFactionOfPlayer(invitee) != null) {
            officer.sendSystemMessage(t("invite.already_in_faction"));
            return false;
        }
        ServerPlayer inviteePlayer = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(invitee);
        if (inviteePlayer != null) {
            faction.addInvite(invitee);
            inviteePlayer.sendSystemMessage(hub.notifier.inviteMessage(faction));
            hub.notifier.toPlayer(inviteePlayer, "faction_invite_" + faction.getUuid(), t("toast.faction_invite.title"), Component.literal(faction.getName()),
                    faction.getColour(), FactionNotifier.DURATION_SIEGE_MS,
                    officer.getEntity() instanceof Player p ? p.getUUID() : null);
        }
        return true;
    }

    /** Accepts the only open invite, or lists them when several. */
    public void acceptInvite(Player player) {
        acceptInvite(player, (String) null);
    }

    public void acceptInvite(Player player, UUID factionId) {
        if (!FactionIds.isValid(factionId)) {
            acceptInvite(player);
            return;
        }
        Faction inviter = hub.registry.getFaction(factionId);
        if (inviter == null || !inviter.isInvitingPlayer(player.getUUID())) {
            player.sendSystemMessage(t("invite.none_from_faction"));
            return;
        }
        completeAccept(player, inviter);
    }

    public void acceptInvite(Player player, String factionName) {
        ArrayList<Faction> invites = getFactionsWithOpenInvitesTo(player.getUUID());
        if (invites.isEmpty()) {
            player.sendSystemMessage(t("invite.none"));
            return;
        }
        Faction inviter = null;
        if (factionName != null && !factionName.isBlank()) {
            for (Faction invite : invites) {
                if (invite.getName().equalsIgnoreCase(factionName.trim())) {
                    inviter = invite;
                    break;
                }
            }
            if (inviter == null) {
                player.sendSystemMessage(t("invite.none_from", factionName));
                return;
            }
        } else if (invites.size() == 1) {
            inviter = invites.get(0);
        } else {
            player.sendSystemMessage(t("invite.multiple"));
            for (Faction invite : invites) {
                player.sendSystemMessage(hub.notifier.inviteMessage(invite));
            }
            return;
        }
        completeAccept(player, inviter);
    }

    private void completeAccept(Player player, Faction inviter) {
        if (hub.registry.getFactionOfPlayer(player.getUUID()) != null) {
            player.sendSystemMessage(t("faction.already_member"));
            return;
        }
        if (isBlockedByRejoinCooldown(player)) {
            return;
        }
        hub.insurance.invalidate(player);
        join(inviter, player.getUUID());
        hub.notifier.memberJoined(inviter, player);
    }

    public boolean requestRemovePlayer(CommandSourceStack remover, UUID factionID, UUID toRemove) {
        Faction faction = hub.registry.getFaction(factionID);
        if (faction == null) {
            remover.sendSystemMessage(t("faction.not_found"));
            return false;
        }
        if (!faction.isPlayerInFaction(toRemove)) {
            remover.sendSystemMessage(t("member.not_in_faction"));
            return false;
        }

        boolean canRemove = Perms.isGameMaster(remover);
        boolean removingSelf = false;
        if (remover.getEntity() instanceof Player removerPlayer) {
            UUID removerID = removerPlayer.getUUID();
            if (removerID.equals(toRemove)) {
                canRemove = true;
                removingSelf = true;
            }
            if (faction.isPlayerOutrankingOfficer(removerID, toRemove)) {
                canRemove = true;
            }
        }
        if (!canRemove) {
            remover.sendSystemMessage(t("member.remove_denied"));
            return false;
        }
        boolean lastMember = faction.getMemberCount() <= 1;
        if (removingSelf && faction.isPlayerRoleInFaction(toRemove, Role.LEADER) && !lastMember) {
            remover.sendSystemMessage(t("member.transfer_first"));
            return false;
        }
        if (lastMember && hub.sieges.isDefending(faction.getUuid())) {
            remover.sendSystemMessage(t("lifecycle.disband_while_defending"));
            return false;
        }

        PlayerRef profile = Profiles.get(WarForgeMod.MC_SERVER, toRemove).orElse(null);
        if (profile == null) {
            remover.sendSystemMessage(t("member.profile_missing"));
        } else if (removingSelf) {
            faction.messageAll(t("member.left", profile.getName(), faction.getName()));
            if (lastMember) {
                faction.messageAll(t("member.abandoned", faction.getName()));
            }
        } else {
            faction.messageAll(t("member.kicked", profile.getName(), faction.getName()));
        }

        leave(faction, toRemove);
        if (profile != null && faction.getMemberCount() > 0) {
            hub.notifier.memberLeft(faction, toRemove, profile.getName(), !removingSelf);
        }

        if (removingSelf) {
            beginRejoinCooldown(toRemove);
            ServerPlayer leaver = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(toRemove);
            long cooldown = getRejoinCooldownRemaining(toRemove);
            if (leaver != null && cooldown > 0L) {
                leaver.sendSystemMessage(t("cooldown.rejoin_self", TimeHelper.formatTime(cooldown)));
            }
        }
        if (faction.getMemberCount() < 1) {
            hub.lifecycle.disband(faction, false);
        }
        hub.siegeSync.sendAllInfoToNearby();
        return true;
    }

    public boolean requestTransferLeadership(Player leader, UUID factionID, UUID newLeaderID) {
        Faction faction = hub.registry.getFaction(factionID);
        if (faction == null) {
            leader.sendSystemMessage(t("faction.not_found"));
            return false;
        }
        if (!FactionAccess.requireRole(leader, faction, Role.LEADER)) {
            return false;
        }
        if (!faction.isPlayerInFaction(newLeaderID)) {
            leader.sendSystemMessage(t("member.not_in_your_faction"));
            return false;
        }
        setLeader(faction, newLeaderID);
        leader.sendSystemMessage(t("member.leader_set"));
        return true;
    }

    public boolean requestPromote(Player leader, ServerPlayer target) {
        return changeRank(leader, target, Role.MEMBER, true);
    }

    public boolean requestDemote(Player leader, ServerPlayer target) {
        return changeRank(leader, target, Role.OFFICER, false);
    }

    private boolean changeRank(Player leader, ServerPlayer target, Role requiredTargetRole, boolean promote) {
        Faction faction = FactionAccess.requireFaction(leader);
        if (faction == null || !FactionAccess.requireRole(leader, faction, Role.LEADER)) {
            return false;
        }
        UUID targetId = target.getUUID();
        if (faction.getRole(targetId) != requiredTargetRole) {
            leader.sendSystemMessage(t(promote ? "member.cannot_promote" : "member.cannot_demote"));
            return false;
        }
        if (promote) {
            faction.promote(targetId);
        } else {
            faction.demote(targetId);
        }
        faction.messageAll(t(promote ? "member.promoted" : "member.demoted", playerName(targetId)));
        FactionTeams.sync(faction);
        return true;
    }

    public void beginRejoinCooldown(UUID playerID) {
        if (WarForgeConfig.FACTION_REJOIN_COOLDOWN_MINUTES > 0) {
            rejoinCooldowns.put(playerID,
                    WarClock.factionRejoinClock() + TimeHelper.minToMs(WarForgeConfig.FACTION_REJOIN_COOLDOWN_MINUTES));
        }
    }

    public long getRejoinCooldownRemaining(UUID playerID) {
        if (WarForgeConfig.FACTION_REJOIN_COOLDOWN_MINUTES <= 0) {
            return 0L;
        }
        Long readyAt = rejoinCooldowns.get(playerID);
        if (readyAt == null) {
            return 0L;
        }
        long remaining = readyAt - WarClock.factionRejoinClock();
        if (remaining <= 0L) {
            rejoinCooldowns.remove(playerID);
            return 0L;
        }
        return remaining;
    }

    public void clearRejoinCooldown(UUID playerID) {
        rejoinCooldowns.remove(playerID);
    }

    /** Ops are exempt; tells the player when blocked. */
    boolean isBlockedByRejoinCooldown(Player player) {
        if (WarForgeMod.isOp(player)) {
            return false;
        }
        long remaining = getRejoinCooldownRemaining(player.getUUID());
        if (remaining <= 0L) {
            return false;
        }
        player.sendSystemMessage(t("cooldown.rejoin", TimeHelper.formatTime(remaining)));
        return true;
    }

    void readFromNBT(CompoundTag tags) {
        rejoinCooldowns.clear();
        for (Tag base : Nbt.getList(tags, "factionRejoinCooldowns", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) base;
            if (Nbt.hasUUID(entry, "player")) {
                rejoinCooldowns.put(Nbt.getUUID(entry, "player"), Nbt.getLong(entry, "readyAt"));
            }
        }
    }

    void writeToNBT(CompoundTag tags) {
        ListTag list = new ListTag();
        long now = WarClock.factionRejoinClock();
        for (Map.Entry<UUID, Long> kvp : rejoinCooldowns.entrySet()) {
            if (kvp.getValue() > now) {
                CompoundTag entry = new CompoundTag();
                Nbt.putUUID(entry, "player", kvp.getKey());
                entry.putLong("readyAt", kvp.getValue());
                list.add(entry);
            }
        }
        tags.put("factionRejoinCooldowns", list);
    }

    public void onLoggedIn(ServerPlayer player) {
        hub.combatLog.onLogin(player.getUUID());
        FactionTeams.playerLoggedIn(player.getUUID());
        Faction faction = hub.registry.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            return;
        }
        faction.setOfflineRaidProtectionUntil(0L);
        hub.notifier.presence(faction, player.getUUID(), player.getName().getString(), true);
    }

    /** Called while the player is still in the player list. */
    public void onLoggedOut(ServerPlayer player) {
        UUID playerId = player.getUUID();
        hub.sieges.removeAttackerPresence(playerId);
        WarForgeMod.TELEPORTS.onLogout(playerId);
        WarForgeMod.FOBS.getWarpQueue().cancel(playerId);
        Faction faction = hub.registry.getFactionOfPlayer(playerId);
        if (faction == null) {
            return;
        }
        hub.notifier.presence(faction, playerId, player.getName().getString(), false);
        if (hub.sieges.getActiveSiegeForFaction(faction.getUuid()) != null) {
            hub.combatLog.onLogout(player);
        }
        if (WarForgeConfig.ENABLE_OFFLINE_RAID_PROTECTION && !faction.isOfflineRaidProtectionDisabled()
                && faction.countOnlineMembers(playerId) == 0) {
            faction.setOfflineRaidProtectionUntil(
                    WarClock.offlineProtectionClock() + TimeUnit.HOURS.toMillis(WarForgeConfig.OFFLINE_RAID_PROTECTION_HOURS));
        }
    }
}
