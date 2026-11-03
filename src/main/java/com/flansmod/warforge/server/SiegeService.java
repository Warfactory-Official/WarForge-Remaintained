package com.flansmod.warforge.server;

import com.flansmod.warforge.common.SiegeMomentum;
import com.flansmod.warforge.common.Notifications;
import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.api.Time;
import com.flansmod.warforge.api.WarforgeAPI;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.IClaim;
import com.flansmod.warforge.common.blocks.SiegeCampBlockEntity;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.TimeHelper;
import com.flansmod.warforge.compat.Inventories;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

public final class SiegeService {
    public enum SiegeTermination {WIN, LOSE, NEUTRAL}

    private static final int FALLBACK_ROUND_SECONDS = 3600;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final int TICKS_PER_SECOND = 20;

    private final FactionStorage hub;
    private final ArrayDeque<DimChunkPos> finished = new ArrayDeque<>(4);

    SiegeService(FactionStorage hub) {
        this.hub = hub;
    }

    /** Round length at a momentum level; a level the config does not define falls back to the highest defined one. */
    public static long momentumMillis(int momentum) {
        Integer seconds = SiegeMomentum.seconds((byte) momentum);
        if (seconds == null) {
            seconds = SiegeMomentum.seconds(SiegeMomentum.max());
        }
        if (seconds == null) {
            WarForgeMod.LOGGER.warn("No siege momentum time configured; using {} s", FALLBACK_ROUND_SECONDS);
            seconds = FALLBACK_ROUND_SECONDS;
        }
        return seconds * MILLIS_PER_SECOND;
    }

    public void advanceDay() {
        pruneInvalid();
        for (var entry : hub.sieges.entries().entrySet()) {
            Siege siege = entry.getValue();
            Faction attackers = hub.registry.getFaction(siege.attackingFaction);
            Faction defenders = hub.registry.getFaction(siege.defendingFaction);
            recalculateBasePower(siege);
            float swing = siege.applyDayElapsed(defenders.hasLoggedInToday(), attackers.hasLoggedInToday());
            announceDayShift(siege, attackers, defenders, swing);
            hub.siegeSync.sendInfoToNearby(entry.getKey());
            WarForgeMod.FOBS.onSiegeTimerReset(siege);
            if (siege.isCompleted()) {
                finished.add(entry.getKey());
            }
        }
        processFinished();
        if (!WarForgeConfig.LEGACY_USES_YIELD_TIMER) {
            hub.registry.increaseLegacy();
        }
    }

    private void announceDayShift(Siege siege, Faction attackers, Faction defenders, float swing) {
        String target = siege.defendingClaim.toFancyString();
        int progress = siege.getAttackProgress();
        int threshold = siege.getAttackSuccessThreshold();
        if (swing > 0) {
            attackers.messageAll(t("siege.day_attacker_favour", defenders.getName(), target, swing, progress, threshold));
            defenders.messageAll(t("siege.day_defender_disfavour", target, attackers.getName(), swing, progress, threshold));
        } else if (swing < 0) {
            defenders.messageAll(t("siege.day_defender_favour", target, attackers.getName(), -swing, progress, threshold));
            attackers.messageAll(t("siege.day_attacker_disfavour", defenders.getName(), target, -swing, progress, threshold));
        } else {
            defenders.messageAll(t("siege.day_none_defender", target, attackers.getName(), progress, threshold));
            attackers.messageAll(t("siege.day_none_attacker", defenders.getName(), target, progress, threshold));
        }
    }

    public void updateTimers() {
        pruneInvalid();
        for (var entry : hub.sieges.entries().entrySet()) {
            Siege siege = entry.getValue();
            boolean roundElapsed = siege.isTimerElapsed();
            if (roundElapsed) {
                Faction attackers = hub.registry.getFaction(siege.attackingFaction);
                siege.startNextRound(momentumMillis(attackers.getSiegeMomentum()));
                WarForgeMod.FOBS.onSiegeTimerReset(siege);
                hub.siegeSync.sendInfoToNearby(entry.getKey());
            } else {
                siege.countDown();
            }
            if (tickCamplessPresence(siege)) {
                notifyAbandoned(hub.registry.getFaction(siege.attackingFaction), hub.registry.getFaction(siege.defendingFaction), true);
                siege.setAttackProgress(-siege.getDefenceThreshold());
                siege.forceOutcome(false);
                finished.add(entry.getKey());
            } else if ((WarForgeConfig.SIEGE_END_ON_GOAL_REACHED || roundElapsed) && siege.isCompleted()) {
                finished.add(entry.getKey());
            }
        }
        processFinished();
    }

    // camp-less: no camp BE enforces presence, so the loop does. true => attackers absent for whole desertion timer
    private boolean tickCamplessPresence(Siege siege) {
        if (!siege.campless || !WarForgeConfig.SIEGE_DECLARE_REQUIRE_PRESENCE
                || siege.attackingCamps.isEmpty() || siege.attackingCamps.get(0) == null) {
            return false;
        }
        int limitTicks = WarForgeConfig.ATTACKER_DESERTION_TIMER * TICKS_PER_SECOND;
        if (limitTicks <= 0) {
            return false;
        }
        DimChunkPos siegePos = siege.defendingClaim.toChunkPos();
        if (siege.hasPresentAttacker()) {
            if (siege.getAttackerAbandonTicks() != 0) {
                siege.setAttackerAbandonTicks(0);
                hub.siegeSync.sendInfoToNearby(siegePos);
            }
            return false;
        }
        int absent = siege.getAttackerAbandonTicks() + 1;
        siege.setAttackerAbandonTicks(absent);
        if (absent % TICKS_PER_SECOND == 0) {
            hub.siegeSync.sendInfoToNearby(siegePos);
        }
        return absent >= limitTicks;
    }

    public void notifyAbandoned(Faction attacking, Faction defending, boolean attackersDeserted) {
        String attackerName = attacking != null ? attacking.getName() : "The attackers";
        String defenderName = defending != null ? defending.getName() : "The defenders";
        FactionNotifier notifier = hub.notifier;
        String token = "warforge.siege_abandoned";
        if (attackersDeserted) {
            notifier.toFaction(attacking, token, t("toast.siege_abandoned.title"), t("toast.siege_abandoned.body"),
                    FactionNotifier.DANGER, FactionNotifier.DURATION_ALERT_MS);
            notifier.toFaction(defending, token, t("toast.siege_repelled.title"), t("toast.siege_repelled.body", attackerName),
                    FactionNotifier.SUCCESS, FactionNotifier.DURATION_ALERT_MS);
            Notifications.messageAll(t("siege.abandoned_by_attackers", attackerName, defenderName));
        } else {
            notifier.toFaction(defending, token, t("toast.defence_abandoned.title"), t("toast.defence_abandoned.body"),
                    FactionNotifier.DANGER, FactionNotifier.DURATION_ALERT_MS);
            notifier.toFaction(attacking, token, t("toast.siege_won.title"), t("toast.defence_abandoned_won.body", defenderName),
                    FactionNotifier.SUCCESS, FactionNotifier.DURATION_ALERT_MS);
            Notifications.messageAll(t("siege.abandoned_by_defenders", defenderName, attackerName));
        }
    }

    public void onPlayerDied(ServerPlayer victim, DamageSource source) {
        ServerPlayer killer = source.getEntity() instanceof ServerPlayer p ? p : null;
        Faction killerFaction = killer != null ? hub.registry.getFactionOfPlayer(killer.getUUID())
                : hub.registry.getFaction(WarforgeAPI.attributedFaction(source));

        if (!hub.sieges.isEmpty()) {
            for (var entry : hub.sieges.entries().entrySet()) {
                onParticipantDeath(entry.getValue(), killer != null ? killer : victim, killerFaction, victim);
                if (WarForgeConfig.SIEGE_END_ON_GOAL_REACHED && entry.getValue().isCompleted()) {
                    finished.add(entry.getKey());
                }
            }
            processFinished();
        }
        if (killerFaction != null) {
            recordKill(killer, killerFaction, victim);
        }
    }

    // killer null: faction-attributed kill (unmanned system); no player to message
    private void recordKill(ServerPlayer killer, Faction killerFaction, ServerPlayer victim) {
        int timesKilled = killerFaction.recordKill(victim.getUUID());
        String victimName = victim.getName().getString();
        if (timesKilled > WarForgeConfig.NOTORIETY_KILL_CAP_PER_PLAYER) {
            if (killer != null) {
                killer.sendSystemMessage(t("kill.capped", victimName, timesKilled));
            }
        } else if (killerFaction != hub.registry.getFactionOfPlayer(victim.getUUID())) {
            if (killer != null) {
                killer.sendSystemMessage(t("kill.notoriety", victimName, WarForgeConfig.NOTORIETY_PER_PLAYER_KILL));
            }
            killerFaction.addNotoriety(WarForgeConfig.NOTORIETY_PER_PLAYER_KILL);
        }
    }

    // PRECISE: only an opposing faction's kill counts, killer in zone (faction-attributed: victim stands in for killer).
    // SIMPLE: any participant death in zone
    private void onParticipantDeath(Siege siege, ServerPlayer zoneWitness, Faction killerFaction, ServerPlayer killed) {
        Faction attackers = hub.registry.getFaction(siege.attackingFaction);
        Faction defenders = hub.registry.getFaction(siege.defendingFaction);
        Faction killedFaction = hub.registry.getFactionOfPlayer(killed.getUUID());

        boolean defenderDied = false;
        boolean attackerDied = false;
        if (WarForgeConfig.SIEGE_KILL_DETECTION_MODE == WarForgeConfig.SiegeKillDetectionMode.SIMPLE) {
            if (isInKillZone(siege, killed)) {
                defenderDied = killedFaction == defenders;
                attackerDied = killedFaction == attackers;
            }
        } else if (killerFaction != null && isInKillZone(siege, zoneWitness)) {
            defenderDied = killerFaction == attackers && killedFaction == defenders;
            attackerDied = killerFaction == defenders && killedFaction == attackers;
        }
        if (!defenderDied && !attackerDied) {
            return;
        }

        int swing = siege.applyParticipantDeath(defenderDied);
        hub.siegeSync.sendInfoToNearby(siege.defendingClaim.toChunkPos());
        Component notification = Component.translatable("warforge.notification.siege_death", killed.getName(), swing,
                siege.getAttackProgress(), siege.getAttackSuccessThreshold(), siege.getDefenceProgress());
        attackers.messageAll(notification);
        defenders.messageAll(notification);
    }

    private static boolean isInKillZone(Siege siege, ServerPlayer player) {
        return siege.isChunkInKillZone(new DimChunkPos(player.level().dimension(), player.blockPosition()));
    }

    public void processFinished() {
        DimChunkPos chunk;
        while ((chunk = finished.poll()) != null) {
            Siege siege = hub.sieges.get(chunk);
            // another path (camp block entity, command) may have concluded it already
            if (siege == null) {
                continue;
            }
            siege.finished = true;
            try {
                conclude(chunk, siege.wasSuccessful() ? SiegeTermination.WIN : SiegeTermination.LOSE, true);
            } catch (RuntimeException e) {
                WarForgeMod.LOGGER.error("Error concluding siege at {}", chunk, e);
            }
        }
    }

    // Single conclusion path. Siege leaves registry first => faction defeat triggered here cannot re-conclude it or block claim removal. cleanupCamps=false when caller camp BE tears itself down.
    public void conclude(DimChunkPos chunk, SiegeTermination termination, boolean cleanupCamps) {
        Siege siege = hub.sieges.remove(chunk);
        if (siege == null) {
            WarForgeMod.LOGGER.warn("Attempted to conclude non-existent siege at {}", chunk);
            return;
        }
        Faction attackers = hub.registry.getFaction(siege.attackingFaction);
        Faction defenders = hub.registry.getFaction(siege.defendingFaction);
        DimBlockPos target = defenders == null ? null : defenders.getSpecificPosForClaim(chunk);
        if (attackers == null || defenders == null || target == null) {
            WarForgeMod.LOGGER.error("Siege at {} concluded with a missing faction or claim; discarded", chunk);
            finish(siege, false, cleanupCamps);
            return;
        }

        String where = target.toFancyString();
        switch (termination) {
            case WIN -> {
                hub.conquered.mark(siege, attackers.getUuid(), WarForgeConfig.ATTACKER_CONQUERED_CHUNK_PERIOD);
                attackers.messageAll(Component.translatable("warforge.info.siege_won_attackers", attackers.getName(), where));
                defenders.messageAll(Component.translatable("warforge.info.siege_lost_defenders", defenders.getName(), where));
                hub.notifier.siegeOutcome(attackers, defenders, target, true);
                attackers.addNotoriety(WarForgeConfig.NOTORIETY_PER_SIEGE_ATTACK_SUCCESS);
                hub.claimService.onClaimLost(defenders, target, true, true);
                if (WarForgeConfig.SIEGE_CAPTURE) {
                    capture(target, attackers);
                }
                increaseMomentum(attackers, true);
            }
            case LOSE -> {
                hub.conquered.mark(siege, defenders.getUuid(), WarForgeConfig.DEFENDER_CONQUERED_CHUNK_PERIOD);
                attackers.messageAll(Component.translatable("warforge.info.siege_lost_attackers", attackers.getName(), where));
                defenders.messageAll(Component.translatable("warforge.info.siege_won_defenders", defenders.getName(), where));
                hub.notifier.siegeOutcome(attackers, defenders, target, false);
                defenders.addNotoriety(WarForgeConfig.NOTORIETY_PER_SIEGE_DEFEND_SUCCESS);
                attackers.stopMomentum();
                attackers.messageAll(t("momentum.lost"));
            }
            case NEUTRAL -> {
                attackers.messageAll(t("siege.cancelled_attackers", attackers.getName(), where));
                defenders.messageAll(t("siege.cancelled_defenders", defenders.getName(), where));
            }
        }
        finish(siege, termination == SiegeTermination.WIN, cleanupCamps);
    }

    private void capture(DimBlockPos target, Faction attackers) {
        ServerLevel level = WarForgeMod.MC_SERVER.getLevel(target.dim);
        if (level == null) {
            WarForgeMod.LOGGER.error("SIEGE_CAPTURE: level null at {}", target);
            return;
        }
        level.setBlockAndUpdate(target.toRegularPos(), Content.basicClaimBlock.defaultBlockState());
        if (level.getBlockEntity(target.toRegularPos()) instanceof IClaim claim) {
            hub.claimService.onNonCitadelClaimPlaced(claim, attackers);
        } else {
            WarForgeMod.LOGGER.error("SIEGE_CAPTURE: no claim block entity at {}", target);
        }
    }

    private void finish(Siege siege, boolean attackersWon, boolean cleanupCamps) {
        siege.finished = true;
        if (cleanupCamps) {
            cleanupCamps(siege, attackersWon);
        }
        hub.siegeSync.broadcastEnded(siege);
        WarForgeMod.FOBS.onDefendingSiegeEnded(siege.defendingFaction);
    }

    private static void cleanupCamps(Siege siege, boolean attackersWon) {
        for (DimBlockPos campPos : siege.attackingCamps) {
            if (campPos == null) {
                continue;
            }
            ServerLevel world = WarForgeMod.MC_SERVER.getLevel(campPos.dim);
            if (world != null && world.getBlockEntity(campPos.toRegularPos()) instanceof SiegeCampBlockEntity camp) {
                if (attackersWon) {
                    camp.cleanupPassedSiege();
                } else {
                    camp.cleanupFailedSiege();
                }
            }
        }
    }

    void terminateInvolving(UUID factionId) {
        ArrayList<DimChunkPos> involved = new ArrayList<>();
        for (var entry : hub.sieges.entries().entrySet()) {
            Siege siege = entry.getValue();
            if (factionId.equals(siege.attackingFaction) || factionId.equals(siege.defendingFaction)) {
                involved.add(entry.getKey());
            }
        }
        for (DimChunkPos chunk : involved) {
            Siege siege = hub.sieges.remove(chunk);
            finish(siege, false, true);
            Faction other = hub.registry.getFaction(factionId.equals(siege.attackingFaction) ? siege.defendingFaction : siege.attackingFaction);
            if (other != null) {
                other.messageAll(t("siege.cancelled_opponent_gone"));
            }
        }
    }

    private void pruneInvalid() {
        ArrayList<DimChunkPos> invalid = null;
        for (var entry : hub.sieges.entries().entrySet()) {
            Siege siege = entry.getValue();
            boolean targetGone = siege.defendingClaim == null
                    || !siege.defendingFaction.equals(hub.claims.getClaim(siege.defendingClaim.toChunkPos()));
            if (targetGone || hub.registry.getFaction(siege.attackingFaction) == null
                    || hub.registry.getFaction(siege.defendingFaction) == null) {
                if (invalid == null) {
                    invalid = new ArrayList<>();
                }
                invalid.add(entry.getKey());
            }
        }
        if (invalid == null) {
            return;
        }
        for (DimChunkPos chunk : invalid) {
            finish(hub.sieges.remove(chunk), false, true);
            WarForgeMod.LOGGER.warn("Pruned dangling siege at {} (participant or target no longer valid)", chunk);
        }
    }

    void increaseMomentum(Faction faction, boolean message) {
        boolean increased = faction.increaseSiegeMomentum();
        if (!message) {
            return;
        }
        if (increased) {
            String nextRound = new Time(momentumMillis(faction.getSiegeMomentum()))
                    .getFormattedTime(Time.TimeFormat.MINUTES_SECONDS, Time.Verbality.FULL);
            MutableComponent level = Component.literal(String.valueOf(faction.getSiegeMomentum()))
                    .setStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD));
            MutableComponent time = Component.literal(nextRound).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            faction.messageAll(t("momentum.increased", level, time));
        } else {
            faction.messageAll(t("momentum.extended", WarForgeConfig.SIEGE_MOMENTUM_DURATION));
        }
    }

    public void requestStartSiege(Player officer, DimBlockPos campPos, Vec3i direction) {
        Faction attacking = FactionAccess.requireFaction(officer);
        if (attacking == null || !checkAttacker(officer, attacking)) {
            return;
        }
        if (direction.getZ() == 0 && direction.getX() == 0) {
            officer.sendSystemMessage(t("siege.not_the_camp"));
            return;
        }
        // the wire carries raw offsets: only cardinal unit steps are valid
        if (Math.abs(direction.getX()) > 1 || Math.abs(direction.getZ()) > 1 || (direction.getX() != 0 && direction.getZ() != 0)) {
            officer.sendSystemMessage(t("siege.bad_direction"));
            return;
        }
        // campPos comes off the wire: neither the dimension nor the block entity is known to exist
        ServerLevel campWorld = WarForgeMod.MC_SERVER.getLevel(campPos.dim);
        if (campWorld == null || !(campWorld.getBlockEntity(campPos.toRegularPos()) instanceof SiegeCampBlockEntity camp)) {
            officer.sendSystemMessage(t("siege.camp_not_found"));
            return;
        }
        if (camp.getSiegeTarget() != null) {
            officer.sendSystemMessage(t("siege.camp_committed"));
            return;
        }
        if (!attacking.getUuid().equals(camp.getFaction())) {
            officer.sendSystemMessage(t("siege.camp_not_owned"));
            return;
        }
        for (Siege siege : hub.sieges.all()) {
            if (siege.attackingCamps.contains(campPos)) {
                officer.sendSystemMessage(t("siege.camp_in_siege"));
                return;
            }
        }

        DimChunkPos defendingChunk = campPos.toChunkPos().offset(direction);
        Faction defending = hub.registry.getFaction(hub.claims.getClaim(defendingChunk));
        if (defending == null) {
            officer.sendSystemMessage(t("siege.no_target_faction"));
            return;
        }
        Component denial = checkTarget(attacking, defending, defendingChunk);
        if (denial != null) {
            officer.sendSystemMessage(denial);
            return;
        }

        DimBlockPos defendingPos = defending.getSpecificPosForClaim(defendingChunk);
        Siege siege = create(attacking, defending, defendingPos, defendingChunk, campPos, false);
        camp.setSiegeTarget(defendingPos);
        start(siege);
        attacking.setLastSiegeTimestamp(WarClock.siegeClock());
    }

    public void requestDeclareSiege(ServerPlayer officer, DimChunkPos targetChunk, DimChunkPos fromChunk) {
        if (!WarForgeConfig.SIEGE_ALLOW_UI_DECLARE) {
            officer.sendSystemMessage(t("siege.declare_disabled"));
            return;
        }
        if (targetChunk == null || fromChunk == null) {
            return;
        }
        Faction attacking = FactionAccess.requireFaction(officer);
        if (attacking == null || !checkAttacker(officer, attacking)) {
            return;
        }
        if (!targetChunk.dim.equals(fromChunk.dim) || !targetChunk.dim.equals(officer.level().dimension())) {
            officer.sendSystemMessage(t("siege.declare_dimension"));
            return;
        }
        int distance = targetChunk.getChessboardDistance(fromChunk);
        if (distance == 0) {
            officer.sendSystemMessage(t("siege.declare_same_chunk"));
            return;
        }
        if (distance > WarForgeConfig.SIEGE_DECLARE_MAX_RANGE) {
            officer.sendSystemMessage(t("siege.declare_range", WarForgeConfig.SIEGE_DECLARE_MAX_RANGE));
            return;
        }
        Faction defending = hub.registry.getFaction(hub.claims.getClaim(targetChunk));
        if (defending == null) {
            officer.sendSystemMessage(t("siege.declare_no_claim"));
            return;
        }
        Component denial = checkTarget(attacking, defending, targetChunk);
        if (denial != null) {
            officer.sendSystemMessage(denial);
            return;
        }
        if (!consumeCampItem(officer)) {
            officer.sendSystemMessage(t("siege.declare_need_item"));
            return;
        }
        DimBlockPos anchor = new DimBlockPos(fromChunk.dim, fromChunk.getMinBlockX(), 0, fromChunk.getMinBlockZ());
        Siege siege = create(attacking, defending, defending.getSpecificPosForClaim(targetChunk), targetChunk, anchor, true);
        start(siege);
        attacking.setLastSiegeTimestamp(WarClock.siegeClock());
    }

    private boolean checkAttacker(Player officer, Faction attacking) {
        long now = WarClock.siegeClock();
        long cooldownEnds = attacking.getLastSiegeTimestamp() + WarForgeConfig.SIEGE_COOLDOWN_FAIL * 60_000L;
        if (attacking.getSiegeMomentum() == 0 && cooldownEnds > now) {
            officer.sendSystemMessage(t("siege.cooldown", TimeHelper.formatTime(cooldownEnds - now)));
            return false;
        }
        if (!FactionAccess.requireRole(officer, attacking, Role.OFFICER)) {
            return false;
        }
        if (WarForgeConfig.MAX_SIEGES > 0 && hub.sieges.countAttacking(attacking.getUuid()) >= WarForgeConfig.MAX_SIEGES) {
            officer.sendSystemMessage(t("siege.max_outgoing", WarForgeConfig.MAX_SIEGES));
            return false;
        }
        return true;
    }

    public Component checkTarget(Faction attacking, Faction defending, DimChunkPos chunk) {
        if (defending.getUuid().equals(attacking.getUuid())) {
            return t("siege.own_claim");
        }
        Component diplomacy = hub.alliances.getSiegeBlockReason(attacking, defending);
        if (diplomacy != null) {
            return diplomacy;
        }
        if (defending.isOfflineRaidProtected()) {
            return t("siege.offline_protected",
                    TimeHelper.formatTime(defending.getOfflineRaidProtectionUntil() - WarClock.offlineProtectionClock()));
        }
        if (defending.isSiegeGraceProtected()) {
            return t("siege.too_new", TimeHelper.formatTime(defending.getSiegeGraceUntil() - WarClock.graceClock()));
        }
        if (WarForgeConfig.MAX_INCOMING_SIEGES > 0 && hub.sieges.countDefending(defending.getUuid()) >= WarForgeConfig.MAX_INCOMING_SIEGES) {
            return t("siege.max_incoming", defending.getName(), WarForgeConfig.MAX_INCOMING_SIEGES);
        }
        DimBlockPos defendingPos = defending.getSpecificPosForClaim(chunk);
        if (defendingPos == null) {
            return t("siege.no_valid_claim");
        }
        ServerLevel level = WarForgeMod.MC_SERVER.getLevel(defendingPos.dim);
        // claims may opt out of being sieged (island collectors, siege camps, ...)
        if (level != null && level.getBlockEntity(defendingPos.toRegularPos()) instanceof IClaim claim && !claim.canBeSieged()) {
            return t("siege.cannot_be_sieged");
        }
        if (hub.sieges.isSiegeInProgress(chunk)) {
            return t("siege.already_under_siege");
        }
        if (hub.conquered.isConquered(chunk)) {
            return Component.translatable("warforge.info.chunk_is_conquered", defending.getName(),
                    TimeHelper.formatTime(hub.conquered.getRemainingMs(chunk)));
        }
        return null;
    }

    private Siege create(Faction attacking, Faction defending, DimBlockPos defendingPos, DimChunkPos defendingChunk,
                         DimBlockPos anchorPos, boolean campless) {
        attacking.forfeitSiegeGrace();
        Siege siege = new Siege(attacking.getUuid(), defending.getUuid(), defendingPos,
                momentumMillis(attacking.getSiegeMomentum()), baseDifficulty(defending, defendingPos));
        siege.setBattleRadius(WarForgeConfig.SIEGE_BATTLE_RADIUS);
        siege.campless = campless;
        siege.attackingCamps.add(anchorPos);
        hub.sieges.put(defendingChunk, siege);
        return siege;
    }

    private int baseDifficulty(Faction defending, DimBlockPos defendingPos) {
        return SiegeDifficulty.baseDefence(WarForgeMod.MC_SERVER, defending, defendingPos.toChunkPos());
    }

    private void start(Siege siege) {
        Faction attackers = hub.registry.getFaction(siege.attackingFaction);
        Faction defenders = hub.registry.getFaction(siege.defendingFaction);
        siege.recalculateEndTimestamp();
        recalculateBasePower(siege);
        initAttackerPresence(siege, attackers);
        Notifications.messageAll(t("siege.started", attackers.getName(), defenders.getName()));
        hub.notifier.siegeStarted(attackers, defenders, siege.defendingClaim);
        hub.siegeSync.sendInfoToNearby(siege.defendingClaim.toChunkPos());
    }

    private void initAttackerPresence(Siege siege, Faction attackers) {
        siege.clearAttackerPresence();
        for (Player player : attackers.getOnlinePlayers(p -> !p.isRemoved())) {
            DimChunkPos chunk = new DimChunkPos(player.level().dimension(), player.blockPosition());
            if (siege.isChunkInAttackerPresenceZone(chunk, hub.claims)) {
                siege.setAttackerPresent(player.getUUID(), true);
            }
        }
    }

    void recalculateBasePower(Siege siege) {
        Faction attackers = hub.registry.getFaction(siege.attackingFaction);
        Faction defenders = hub.registry.getFaction(siege.defendingFaction);
        DimChunkPos defendingChunk = siege.defendingClaim.toChunkPos();
        int extra = SiegeDifficulty.memberBonus(defenders)
                - SiegeDifficulty.adjacentSupport(WarForgeMod.MC_SERVER, defenders, defendingChunk)
                + WarforgeAPI.getReinforcementBonus(siege.defendingFaction, defendingChunk.dim, defendingChunk.toChunkPos());

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            DimChunkPos neighbour = defendingChunk.offset(direction, 1);
            if (hub.claims.getClaim(neighbour).equals(siege.attackingFaction)) {
                IClaim claim = claimAt(attackers.getSpecificPosForClaim(neighbour));
                if (claim != null) {
                    extra += claim.getAttackStrength();
                }
            }
        }
        siege.setExtraDifficulty(extra);
    }

    private static IClaim claimAt(DimBlockPos pos) {
        if (pos == null) {
            return null;
        }
        ServerLevel level = WarForgeMod.MC_SERVER.getLevel(pos.dim);
        BlockEntity entity = level == null ? null : level.getBlockEntity(pos.toRegularPos());
        return entity instanceof IClaim claim ? claim : null;
    }

    private static boolean consumeCampItem(ServerPlayer player) {
        if (Content.siegeCampBlockItem == null) {
            return true;
        }
        var main = Inventories.main(player.getInventory());
        for (int i = 0; i < main.size(); i++) {
            ItemStack stack = main.get(i);
            if (!stack.isEmpty() && stack.getItem() == Content.siegeCampBlockItem) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    main.set(i, ItemStack.EMPTY);
                }
                player.inventoryMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }
}
