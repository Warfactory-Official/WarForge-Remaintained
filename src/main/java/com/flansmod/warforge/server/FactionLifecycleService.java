package com.flansmod.warforge.server;

import com.flansmod.warforge.common.PlayerSyncService;
import com.flansmod.warforge.common.Notifications;
import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.IClaim;
import com.flansmod.warforge.common.blocks.CitadelBlockEntity;
import com.flansmod.warforge.common.blocks.ClaimBlockEntity;
import com.flansmod.warforge.common.EffectPackets;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.FactionDisplay;
import com.flansmod.warforge.compat.Players;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.flansmod.warforge.server.ServerText.t;

/** Founding, renaming, flag and colour changes, disbanding and defeat. */
public final class FactionLifecycleService {
    private static final int WHITE = 0xffffff;
    private final FactionStorage hub;

    FactionLifecycleService(FactionStorage hub) {
        this.hub = hub;
    }

    /** @return false after telling the sender why */
    public boolean validateName(String name, CommandSourceStack sender, UUID existingFactionId) {
        if (name == null || name.isEmpty()) {
            sender.sendSystemMessage(t("name.empty"));
            return false;
        }
        if (name.length() > WarForgeConfig.FACTION_NAME_LENGTH_MAX) {
            sender.sendSystemMessage(t("name.too_long", WarForgeConfig.FACTION_NAME_LENGTH_MAX));
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!('0' <= c && c <= '9') && !('a' <= c && c <= 'z') && !('A' <= c && c <= 'Z')) {
                sender.sendSystemMessage(t("name.bad_char", String.valueOf(c)));
                return false;
            }
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (String banned : WarForgeConfig.FACTION_NAME_BANLIST) {
            if (banned != null && !banned.isBlank() && lowerName.contains(banned.trim().toLowerCase(Locale.ROOT))) {
                sender.sendSystemMessage(t("name.banned"));
                return false;
            }
        }
        if (hub.registry.isNameTaken(name, existingFactionId)) {
            sender.sendSystemMessage(t("name.taken", name));
            return false;
        }
        return true;
    }

    public boolean requestCreate(CitadelBlockEntity citadel, ServerPlayer player, String factionName, int colour) {
        // Server-side authority: a crafted create packet skips the GUI, which is the only other place
        // these rules are enforced. Without them any factionless player could take over an enemy citadel.
        if (citadel == null) {
            player.sendSystemMessage(t("create.no_citadel"));
            return false;
        }
        if (!citadel.getFaction().equals(FactionIds.NONE)) {
            player.sendSystemMessage(t("create.citadel_owned"));
            return false;
        }
        DimChunkPos citadelChunk = citadel.getClaimPos().toChunkPos();
        if (hub.claims.isClaimed(citadelChunk)) {
            player.sendSystemMessage(t("create.chunk_claimed"));
            return false;
        }
        if (!WarForgeMod.isOp(player) && !player.getUUID().equals(citadel.placer)) {
            player.sendSystemMessage(t("create.not_placer"));
            return false;
        }
        if (hub.sieges.isChunkContested(citadelChunk)) {
            player.sendSystemMessage(t("claim.contested"));
            return false;
        }
        if (!validateName(factionName, Players.commandSource(player), null)) {
            return false;
        }
        if (hub.registry.getFactionOfPlayer(player.getUUID()) != null) {
            player.sendSystemMessage(t("faction.already_member"));
            return false;
        }
        if (hub.membership.isBlockedByRejoinCooldown(player)) {
            return false;
        }

        hub.insurance.invalidate(player);

        UUID id = FactionIds.createUUID(factionName);
        while (hub.registry.contains(id)) {
            id = UUID.randomUUID();
        }
        if (colour == WHITE) {
            colour = Color.HSBtoRGB(WarForgeMod.rand.nextFloat(), WarForgeMod.rand.nextFloat() * 0.5f + 0.5f, 1.0f);
        }

        Faction faction = new Faction(id, factionName, colour, new DimBlockPos(citadel));
        hub.registry.register(faction);
        hub.claims.claim(faction, faction.getCitadelPos(), ClaimType.CITADEL);
        if (WarForgeConfig.ENABLE_SIEGE_GRACE_PERIOD && WarForgeConfig.SIEGE_GRACE_PERIOD_HOURS > 0) {
            faction.grantSiegeGrace(WarClock.graceClock() + TimeUnit.HOURS.toMillis(WarForgeConfig.SIEGE_GRACE_PERIOD_HOURS));
        }
        citadel.onServerCreateFaction(faction);

        Notifications.messageAll(t("create.announce", player.getName().getString(), factionName));
        hub.membership.join(faction, player.getUUID());
        hub.membership.setLeader(faction, player.getUUID());
        hub.namePlates.broadcast(player, factionName, faction.getColour());
        return true;
    }

    public boolean requestRename(CommandSourceStack sender, UUID factionId, String newName) {
        Faction faction = hub.registry.getFaction(factionId);
        if (faction == null) {
            sender.sendSystemMessage(t("faction.not_found"));
            return false;
        }
        if (!validateName(newName, sender, faction.getUuid())) {
            return false;
        }
        String oldName = faction.getName();
        hub.registry.rename(faction, newName);
        forEachOwnedClaim(faction, ClaimBlockEntity.class, claim -> claim.updateFactionName(newName));
        hub.namePlates.broadcastFaction(faction);
        FactionDisplay.refreshFactionTabNames(faction);
        FactionTeams.sync(faction);
        Notifications.messageAll(t("rename.announce", oldName, newName));
        return true;
    }

    public boolean requestSetColour(ServerPlayer player, int colour) {
        Faction faction = FactionAccess.requireFaction(player);
        if (faction == null || !FactionAccess.requireRole(player, faction, Role.LEADER)) {
            return false;
        }
        faction.setColour(colour);
        forEachOwnedClaim(faction, IClaim.class, claim -> claim.updateColour(colour));
        // push now so a reopened citadel panel shows the new accent immediately
        PlayerSyncService.syncClaimToPlayer(player, faction.getCitadelPos().toRegularPos());
        hub.namePlates.broadcastFaction(faction);
        FactionTeams.sync(faction);
        return true;
    }

    public boolean requestChooseFlag(ServerPlayer player, UUID factionId, String flagId) {
        Faction faction = hub.registry.getFaction(factionId);
        if (faction == null) {
            player.sendSystemMessage(t("faction.not_found"));
            return false;
        }
        if (!FactionAccess.requireRole(player, faction, Role.LEADER)) {
            return false;
        }
        if (!faction.getFlagId().isEmpty()) {
            player.sendSystemMessage(t("flag.already_chosen"));
            return false;
        }
        if (!WarForgeMod.FLAG_REGISTRY.isAvailable(flagId, player.getUUID())) {
            player.sendSystemMessage(t("flag.unavailable"));
            return false;
        }
        if (WarForgeConfig.UNIQUE_FLAGS && isFlagTakenByOtherFaction(flagId, faction.getUuid())) {
            player.sendSystemMessage(t("flag.taken"));
            return false;
        }
        applyFlag(faction, flagId);
        PlayerSyncService.syncClaimToPlayer(player, faction.getCitadelPos().toRegularPos());
        faction.messageAll(t("flag.selected", flagId));
        return true;
    }

    /** Admin override: skips the unique-flag rule. */
    public boolean adminSetFlag(CommandSourceStack src, UUID factionId, String flagId) {
        Faction faction = hub.registry.getFaction(factionId);
        if (faction == null) {
            src.sendFailure(t("faction.not_found"));
            return false;
        }
        String newFlagId = flagId == null ? "" : flagId;
        if (!newFlagId.isEmpty() && !WarForgeMod.FLAG_REGISTRY.isAvailable(newFlagId)) {
            src.sendFailure(t("flag.unavailable_id", newFlagId));
            return false;
        }
        applyFlag(faction, newFlagId);
        if (newFlagId.isEmpty()) {
            src.sendSuccess(() -> t("flag.admin_reset_done", faction.getName()), true);
            faction.messageAll(t("flag.admin_reset"));
        } else {
            src.sendSuccess(() -> t("flag.admin_set_done", faction.getName(), newFlagId), true);
            faction.messageAll(t("flag.admin_set", newFlagId));
        }
        return true;
    }

    public boolean isFlagTakenByOtherFaction(String flagId, UUID excludeFactionId) {
        if (flagId == null || flagId.isEmpty()) {
            return false;
        }
        for (Faction other : hub.registry.getAllFactions()) {
            if (!other.getUuid().equals(excludeFactionId) && flagId.equalsIgnoreCase(other.getFlagId())) {
                return true;
            }
        }
        return false;
    }

    private void applyFlag(Faction faction, String flagId) {
        faction.setFlagId(flagId);
        forEachOwnedClaim(faction, ClaimBlockEntity.class, claim -> claim.updateFactionFlag(flagId));
        for (ServerPlayer online : WarForgeMod.MC_SERVER.getPlayerList().getPlayers()) {
            ClaimMapView.send(online, new DimChunkPos(online.level().dimension(), online.blockPosition()),
                    WarForgeConfig.CLAIM_MANAGER_RADIUS);
        }
    }

    /** Loaded claim block entities of the faction at its claim, collector and attacking-camp positions. */
    private <T extends IClaim> void forEachOwnedClaim(Faction faction, Class<T> type, Consumer<T> action) {
        for (DimBlockPos pos : hub.claimService.collectFactionPositions(faction)) {
            ServerLevel level = WarForgeMod.MC_SERVER.getLevel(pos.dim);
            if (level != null && type.isInstance(level.getBlockEntity(pos.toRegularPos()))) {
                T claim = type.cast(level.getBlockEntity(pos.toRegularPos()));
                if (claim.getFaction().equals(faction.getUuid())) {
                    action.accept(claim);
                }
            }
        }
    }

    public boolean requestDisband(Player leader, UUID factionId) {
        Faction faction = FactionIds.isValid(factionId)
                ? hub.registry.getFaction(factionId)
                : hub.registry.getFactionOfPlayer(leader.getUUID());
        if (faction == null || !faction.isPlayerRoleInFaction(leader.getUUID(), Role.LEADER)) {
            leader.sendSystemMessage(t("role.leader_required"));
            return false;
        }
        if (hub.sieges.isDefending(faction.getUuid())) {
            leader.sendSystemMessage(t("lifecycle.disband_while_defending"));
            return false;
        }
        disband(faction, false);
        return true;
    }

    /** Siege loss of the citadel, or admin disband: insurance is unlocked for the leader. */
    public void defeat(Faction faction) {
        disband(faction, true);
    }

    void disband(Faction faction, boolean unlockInsurance) {
        UUID id = faction.getUuid();
        // end sieges first so no siege or ticking camp keeps pointing at a dead faction
        hub.siegeService.terminateInvolving(id);
        if (unlockInsurance) {
            hub.insurance.unlock(faction);
        }
        EffectPackets.disband(faction.getCitadelPos().dim, faction.getCitadelPos().toRegularPos(), 100);
        hub.namePlates.broadcastRemoval(faction);

        // index first: block removal can re-enter claim lookups
        hub.claims.releaseAll(faction);
        removeBlocks(faction);

        hub.notifier.disbanded(faction);
        faction.messageAll(t(faction.getMemberCount() > 0 ? "lifecycle.disbanded" : "lifecycle.abandoned", faction.getName()));
        ArrayList<UUID> formerMembers = new ArrayList<>(faction.getMemberIds());
        faction.clearMembers();
        FactionTeams.dissolve(id, formerMembers);
        for (UUID member : formerMembers) {
            hub.registry.unindexMember(member);
            FactionDisplay.refreshTabName(member);
        }
        faction.clearClaims();
        faction.clearForcedChunks();
        faction.clearIslandCollectors();
        faction.clearInsurance();
        WarForgeMod.FOBS.destroyFobsOf(id);

        hub.registry.unregister(faction);
        hub.alliances.removeFactionEverywhere(id);
        hub.conquered.forgetFaction(id);
        for (Faction other : hub.registry.getAllFactions()) {
            for (UUID member : formerMembers) {
                other.forgetKillsOf(member);
            }
        }
        WarForgeMod.CHUNK_LOADING_MANAGER.releaseFaction(id);
    }

    private void removeBlocks(Faction faction) {
        for (DimBlockPos pos : faction.getClaimPositions()) {
            removeBlock(pos);
        }
        removeBlock(faction.getCitadelPos());
        for (DimBlockPos collector : faction.getIslandCollectors()) {
            removeBlock(collector);
        }
    }

    private static void removeBlock(DimBlockPos pos) {
        ServerLevel world = WarForgeMod.MC_SERVER.getLevel(pos.dim);
        if (world != null) {
            world.removeBlock(pos.toRegularPos(), false);
        }
    }
}
