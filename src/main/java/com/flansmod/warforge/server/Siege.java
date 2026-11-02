package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.faction.ChunkRadius;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Nbt;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Siege state and pure progress math; orchestration lives in {@link SiegeService}. */
public class Siege {
    private static final long TICK_MS = 50L;
    private static final int MAX_STALL_MULTIPLIER = 1024;
    private static final int DEFAULT_BASE_DIFFICULTY = 5;
    private static final long CLOCK_DRIFT_RESYNC_MS = 1000L;

    public UUID attackingFaction;
    public UUID defendingFaction;
    public final ArrayList<DimBlockPos> attackingCamps = new ArrayList<>(4);
    public DimBlockPos defendingClaim;
    /** Camp-less (map-declared) sieges anchor logically at attackingCamps[0]; presence is enforced by the siege loop. */
    public boolean campless = false;
    /** Not persisted. */
    public boolean finished = false;

    @Getter
    private long timeRemainingMillis;
    @Getter
    private long siegeEndTimeStamp = 999L;
    private int baseDifficulty = DEFAULT_BASE_DIFFICULTY;
    private int extraDifficulty = 0;
    /** In [-defenceThreshold, attackSuccessThreshold]; sign = who is winning. */
    private int attackProgress = 0;
    private int stallMultiplier = 1;
    @Getter
    private int battleRadius = WarForgeConfig.SIEGE_BATTLE_RADIUS;
    @Getter
    private int siegedRadius = WarForgeConfig.SIEGE_SIEGED_RADIUS;
    private final HashSet<UUID> presentAttackers = new HashSet<>();
    private Boolean forcedAttackerVictory = null;

    // persisted so a restart does not reset them
    @Getter
    private int attackerAbandonTicks = 0;
    @Getter
    private int defenderAbandonTicks = 0;
    @Getter
    private long defenderOfflineTimerMs = 0;
    @Getter
    private int largestSeenDefenderCount = 0;

    private Set<DimChunkPos> islandCache;
    private int islandCacheRevision = -1;

    public Siege() {
    }

    public Siege(UUID attacker, UUID defender, DimBlockPos defending, long timeMillis, int baseDifficulty) {
        attackingFaction = attacker;
        defendingFaction = defender;
        defendingClaim = defending;
        timeRemainingMillis = timeMillis;
        this.baseDifficulty = baseDifficulty;
    }

    public int getAttackProgress() {
        return attackProgress;
    }

    public int getDefenceProgress() {
        return -attackProgress;
    }

    public void setAttackProgress(int progress) {
        attackProgress = progress;
    }

    public int getAttackSuccessThreshold() {
        return Math.max(1, baseDifficulty + extraDifficulty);
    }

    public int getDefenceThreshold() {
        return Math.max(1, WarForgeConfig.SIEGE_DEFENCE_THRESHOLD);
    }

    void setExtraDifficulty(int extraDifficulty) {
        this.extraDifficulty = extraDifficulty;
    }

    public boolean isCompleted() {
        return hasForcedOutcome()
                || getAttackProgress() >= getAttackSuccessThreshold()
                || getDefenceProgress() >= getDefenceThreshold();
    }

    public void forceOutcome(boolean attackersWin) {
        forcedAttackerVictory = attackersWin;
    }

    public boolean hasForcedOutcome() {
        return forcedAttackerVictory != null;
    }

    public boolean wasSuccessful() {
        if (forcedAttackerVictory != null) {
            return forcedAttackerVictory;
        }
        return getAttackProgress() >= getAttackSuccessThreshold();
    }

    /** @return signed change applied to attack progress (positive when a defender died) */
    int applyParticipantDeath(boolean defenderDied) {
        stallMultiplier = 1;
        int swing = defenderDied
                ? WarForgeConfig.SIEGE_SWING_PER_DEFENDER_DEATH
                : -WarForgeConfig.SIEGE_SWING_PER_ATTACKER_DEATH;
        attackProgress += swing;
        return swing;
    }

    /** @return swing added to progress */
    float applyDayElapsed(boolean defendersLoggedIn, boolean attackersLoggedIn) {
        float swing = WarForgeConfig.SIEGE_SWING_PER_DAY_ELAPSED_BASE;
        if (!defendersLoggedIn) {
            swing += WarForgeConfig.SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS;
        }
        if (!attackersLoggedIn) {
            swing -= WarForgeConfig.SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS;
        }
        attackProgress += swing;
        return swing;
    }

    boolean isTimerElapsed() {
        return timeRemainingMillis <= 0;
    }

    /** Round boundary: base swing (escalating while stalled), then a fresh round. */
    void startNextRound(long nextRoundMillis) {
        int swing = WarForgeConfig.SIEGE_SWING_PER_DAY_ELAPSED_BASE;
        if (getDefenceProgress() > WarForgeConfig.SIEGE_STALL_ESCALATION_THRESHOLD) {
            swing *= stallMultiplier;
            stallMultiplier = Math.min(stallMultiplier * 2, MAX_STALL_MULTIPLIER);
        } else {
            stallMultiplier = 1;
        }
        attackProgress += swing;
        timeRemainingMillis = nextRoundMillis;
        recalculateEndTimestamp();
    }

    void countDown() {
        timeRemainingMillis -= TICK_MS;
        long actualRemaining = siegeEndTimeStamp - WarClock.siegeClock();
        if (Math.abs(actualRemaining - timeRemainingMillis) > CLOCK_DRIFT_RESYNC_MS) {
            timeRemainingMillis = actualRemaining;
        }
    }

    void recalculateEndTimestamp() {
        siegeEndTimeStamp = WarClock.siegeClock() + timeRemainingMillis;
    }

    public void setBattleRadius(int battleRadius) {
        this.battleRadius = Math.max(0, battleRadius);
    }

    public void setSiegedRadius(int siegedRadius) {
        this.siegedRadius = Math.max(0, siegedRadius);
    }

    public boolean isChunkInBattleZone(DimChunkPos chunk) {
        return isNearAnyCamp(chunk, battleRadius);
    }

    public boolean isChunkInSiegedZone(DimChunkPos chunk) {
        return isNearAnyCamp(chunk, siegedRadius);
    }

    /** Kills and deaths count in the union of War and Sieged zones. */
    boolean isChunkInKillZone(DimChunkPos chunk) {
        return isNearAnyCamp(chunk, Math.max(battleRadius, siegedRadius));
    }

    /** Camp radius, or the defender island (attackers inside the sieged claims count as present). */
    boolean isChunkInAttackerPresenceZone(DimChunkPos chunk, ClaimRegistry claims) {
        if (isNearAnyCamp(chunk, WarForgeConfig.SIEGE_ATTACKER_RADIUS)) {
            return true;
        }
        return defendingFaction.equals(claims.getClaim(chunk)) && defenderIsland(claims).contains(chunk);
    }

    /** Defender island cached per claim revision. */
    private Set<DimChunkPos> defenderIsland(ClaimRegistry claims) {
        if (islandCache == null || islandCacheRevision != claims.getRevision()) {
            islandCache = claims.collectIsland(defendingFaction, defendingClaim.toChunkPos());
            islandCacheRevision = claims.getRevision();
        }
        return islandCache;
    }

    private boolean isNearAnyCamp(DimChunkPos chunk, int radius) {
        for (DimBlockPos camp : attackingCamps) {
            if (camp != null && ChunkRadius.contains(camp.toChunkPos(), chunk, radius)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPresentAttacker() {
        return !presentAttackers.isEmpty();
    }

    public void setAttackerPresent(UUID playerId, boolean present) {
        if (present) {
            presentAttackers.add(playerId);
        } else {
            presentAttackers.remove(playerId);
        }
    }

    void clearAttackerPresence() {
        presentAttackers.clear();
    }

    public void setAttackerAbandonTicks(int ticks) {
        attackerAbandonTicks = ticks;
    }

    public void setDefenderAbandonTicks(int ticks) {
        defenderAbandonTicks = ticks;
    }

    public void setDefenderOfflineTimerMs(long ms) {
        defenderOfflineTimerMs = ms;
    }

    public void setLargestSeenDefenderCount(int count) {
        largestSeenDefenderCount = count;
    }

    public void readFromNBT(CompoundTag tags) {
        attackingCamps.clear();
        attackingFaction = Nbt.getUUID(tags, "attacker");
        defendingFaction = Nbt.getUUID(tags, "defender");
        for (Tag base : Nbt.getList(tags, "attackLocations", Tag.TAG_COMPOUND)) {
            attackingCamps.add(DimBlockPos.readFromNBT((CompoundTag) base));
        }
        defendingClaim = DimBlockPos.readFromNBT(tags, "defendLocation");
        battleRadius = tags.contains("battleRadius") ? Math.max(0, Nbt.getInt(tags, "battleRadius")) : WarForgeConfig.SIEGE_BATTLE_RADIUS;
        siegedRadius = tags.contains("siegedRadius") ? Math.max(0, Nbt.getInt(tags, "siegedRadius")) : WarForgeConfig.SIEGE_SIEGED_RADIUS;
        attackProgress = Nbt.getInt(tags, "progress");
        baseDifficulty = Nbt.getInt(tags, "baseDifficulty");
        extraDifficulty = Nbt.getInt(tags, "extraDifficulty");
        campless = Nbt.getBoolean(tags, "campless");
        attackerAbandonTicks = Nbt.getInt(tags, "attackerAbsenceTicks");
        defenderAbandonTicks = Nbt.getInt(tags, "defenderAbandonTicks");
        defenderOfflineTimerMs = Nbt.getLong(tags, "defenderOfflineTimerMs");
        largestSeenDefenderCount = Nbt.getInt(tags, "largestSeenDefenderCount");
        // "timeElapsed" holds the time remaining
        timeRemainingMillis = Nbt.getLong(tags, "timeElapsed");
        recalculateEndTimestamp();
    }

    public void writeToNBT(CompoundTag tags) {
        Nbt.putUUID(tags, "attacker", attackingFaction);
        Nbt.putUUID(tags, "defender", defendingFaction);

        ListTag claimsList = new ListTag();
        for (DimBlockPos pos : attackingCamps) {
            claimsList.add(pos.writeToNBT());
        }
        tags.put("attackLocations", claimsList);
        tags.put("defendLocation", defendingClaim.writeToNBT());
        tags.putInt("battleRadius", battleRadius);
        tags.putInt("siegedRadius", siegedRadius);
        tags.putInt("progress", attackProgress);
        tags.putInt("baseDifficulty", baseDifficulty);
        tags.putInt("extraDifficulty", extraDifficulty);
        tags.putLong("timeElapsed", timeRemainingMillis);
        tags.putBoolean("campless", campless);
        tags.putInt("attackerAbsenceTicks", attackerAbandonTicks);
        tags.putInt("defenderAbandonTicks", defenderAbandonTicks);
        tags.putLong("defenderOfflineTimerMs", defenderOfflineTimerMs);
        tags.putInt("largestSeenDefenderCount", largestSeenDefenderCount);
    }
}
