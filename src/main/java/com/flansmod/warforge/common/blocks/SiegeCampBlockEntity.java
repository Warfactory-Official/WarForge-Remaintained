package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.ChunkRadius;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.PacketSiegeCampProgressUpdate;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.TimeHelper;
import com.flansmod.warforge.compat.Nbt;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.Siege;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.UUID;

import static com.flansmod.warforge.common.WarForgeConfig.SIEGE_DEFENDER_RADIUS;

public class SiegeCampBlockEntity extends ClaimBlockEntity {
    private UUID placer = FactionIds.NONE;
    private DimBlockPos siegeTarget = null;
    private UUID defendersId = FactionIds.NONE;
    private int tickTimer = 0;
    private SiegeStatus siegeStatus = SiegeStatus.IDLING;
    private int lastSyncedAbandonTimer = -1;
    private long previousTimestamp = WarClock.currTickTimestamp;
    private int lastSeenDefenderCount;

    public SiegeCampBlockEntity(BlockPos pos, BlockState state) {
        super(Content.TE_SIEGE_CAMP.get(), pos, state);
    }

    @Override
    public void onPlacedBy(LivingEntity placer) {
        this.placer = placer.getUUID();
        super.onPlacedBy(placer);
    }

    @Override
    public int getDefenceStrength() { return 0; }

    @Override
    public int getSupportStrength() { return 0; }

    @Override
    public int getAttackStrength() { return WarForgeConfig.ATTACK_STRENGTH_SIEGE_CAMP; }

    private Siege siege() {
        return siegeTarget == null ? null : WarForgeMod.FACTIONS.sieges.entries().get(siegeTarget.toChunkPos());
    }

    public void setSiegeTarget(DimBlockPos siegeTarget) {
        Faction defenders = getDefenders(siegeTarget);
        if (defenders == null) {
            WarForgeMod.LOGGER.error("Siege camp at {} could not bind to target {} (no defending faction); aborting bind.", getClaimPos(), siegeTarget);
            this.siegeTarget = null;
            return;
        }
        this.siegeTarget = siegeTarget;
        defendersId = defenders.getUuid();
        siegeStatus = SiegeStatus.ACTIVE;
        setChanged();
    }

    public DimBlockPos getSiegeTarget() {
        return siegeTarget;
    }

    private static Faction getDefenders(DimBlockPos siegeTarget) {
        return WarForgeMod.FACTIONS.registry.getFaction(WarForgeMod.FACTIONS.claims.getClaim(siegeTarget));
    }

    private Faction defenders() {
        return WarForgeMod.FACTIONS.registry.getFaction(defendersId);
    }

    private static boolean doCheckPerTick() {
        return WarForgeConfig.ATTACKER_DESERTION_TIMER == 0 || WarForgeConfig.DEFENDER_DESERTION_TIMER == 0;
    }

    @Override
    public boolean canBeSieged() { return false; }

    private enum SiegeStatus {
        IDLING,
        ACTIVE,
        FAILED,
        PASSED,
        FAILED_CLEANUP,
        PASSED_CLEANUP;

        private static final SiegeStatus[] VALUES = values();

        boolean isCleanup() { return this == FAILED_CLEANUP || this == PASSED_CLEANUP; }

        boolean isPassed() { return this == PASSED || this == PASSED_CLEANUP; }

        boolean isFailed() { return this == FAILED || this == FAILED_CLEANUP; }
    }

    public void failSiege() {
        siegeStatus = SiegeStatus.FAILED;
        concludeSiege();
    }

    public void cleanupFailedSiege() {
        siegeStatus = SiegeStatus.FAILED_CLEANUP;
        concludeSiege();
    }

    public void passSiege() {
        siegeStatus = SiegeStatus.PASSED;
        concludeSiege();
    }

    public void cleanupPassedSiege() {
        siegeStatus = SiegeStatus.PASSED_CLEANUP;
        concludeSiege();
    }

    private void concludeSiege() {
        if (level.isClientSide()) {
            return;
        }

        if (siegeTarget == null) {
            WarForgeMod.LOGGER.error("Siege conclusion error! siegeTarget is null!");
            return;
        }
        Siege siege = WarForgeMod.FACTIONS.sieges.entries().get(siegeTarget.toChunkPos());
        if (!siegeStatus.isCleanup() && siege != null) {
            SiegeCampProgressInfo info = WarForgeMod.FACTIONS.siegeSync.buildInfo(siege);
            if (info != null) {
                info.progress = siegeStatus.isFailed() ? -5 : info.completionPoint;
                PacketSiegeCampProgressUpdate packet = new PacketSiegeCampProgressUpdate();
                packet.info = info;

                for (Player attacker : getAttacking().getOnlinePlayers(Objects::nonNull))
                    WarForgeMod.NETWORK.sendTo(packet, (ServerPlayer) attacker);
                for (Player defender : defenders().getOnlinePlayers(Objects::nonNull))
                    WarForgeMod.NETWORK.sendTo(packet, (ServerPlayer) defender);
            }

            try {
                siege.setAttackProgress(siegeStatus.isFailed() ? -5 : siege.getAttackSuccessThreshold());
                siege.forceOutcome(!siegeStatus.isFailed());
                WarForgeMod.FACTIONS.siegeService.conclude(siegeTarget.toChunkPos(),
                        siegeStatus.isFailed() ? com.flansmod.warforge.server.SiegeService.SiegeTermination.LOSE : com.flansmod.warforge.server.SiegeService.SiegeTermination.WIN, false);
            } catch (RuntimeException e) {
                WarForgeMod.LOGGER.warn("Failed to force end siege of target {} at {}", siegeTarget, getClaimPos(), e);
            }

            for (DimBlockPos siegeCampPos : siege.attackingCamps) {
                if (siegeCampPos == null || getClaimPos().equals(siegeCampPos.toRegularPos())) continue;

                BlockEntity siegeCamp = level.getBlockEntity(siegeCampPos.toRegularPos());
                if (!(siegeCamp instanceof SiegeCampBlockEntity camp)) continue;

                if (siegeStatus.isFailed()) camp.cleanupFailedSiege();
                else if (siegeStatus.isPassed()) camp.cleanupPassedSiege();
            }
        }

        siegeTarget = null;
        defendersId = FactionIds.NONE;
        destroy();
    }

    public void destroy() {
        WarForgeMod.FACTIONS.claimService.requestRemoveClaimServer(getClaimPos());
    }

    public void tick() {
        if (level.isClientSide()) return;

        if (!(level.getBlockState(worldPosition).getBlock() instanceof BlockSiegeCamp)) {
            destroy();
            return;
        }

        Faction defenders = defenders();
        Siege siege = siege();
        if (placer.equals(FactionIds.NONE) || siege == null || defenders == null) return;

        tickTimer &= Integer.MAX_VALUE;

        boolean checkPerTick = doCheckPerTick();
        int timerStep = checkPerTick ? 1 : 20;
        if (checkPerTick || tickTimer % 20 == 0) {
            if (tickTimer % 6000 == 0) {
                messageAllDefenders(defenders, "warforge.info.siege_defense_info", new DimBlockPos(level.dimension(), getClaimPos()).toFancyString());
            }

            boolean attackersPresent = siege.hasPresentAttacker();
            if (!attackersPresent) {
                if (handleDesertion(siege, true, checkPerTick)) return;
            } else {
                siege.setAttackerAbandonTicks(Math.max(0, siege.getAttackerAbandonTicks() - timerStep));
            }

            if (tickTimer % 20 == 0 && siege.getAttackerAbandonTicks() != lastSyncedAbandonTimer) {
                lastSyncedAbandonTimer = siege.getAttackerAbandonTicks();
                WarForgeMod.FACTIONS.siegeSync.sendInfoToNearby(siegeTarget.toChunkPos());
            }

            if (lastSeenDefenderCount == 0 && defenders.countOnlineMembers(null) > 0 && siege.getDefenderOfflineTimerMs() > 0) {
                defenders.messageAll(Component.translatable("warforge.common.siege_camp.offline_timer",
                        defenders.getName(), TimeHelper.formatTime(siege.getDefenderOfflineTimerMs()), getClaimPos().toString()));
            }

            lastSeenDefenderCount = defenders.countOnlineMembers(null);

            if (defenders.countOnlineMembers(null) > siege.getLargestSeenDefenderCount()) siege.setLargestSeenDefenderCount(defenders.countOnlineMembers(null));
            int numActiveDefenders = defenders.getOnlinePlayers(this::isDefenderInWarzone).size();

            long msPassed = WarClock.currTickTimestamp - previousTimestamp;
            if (attackersPresent && haveDefendersLiveQuit(siege, defenders)) {
                incrementOfflineTimer(siege, msPassed);
                if (siege.getDefenderOfflineTimerMs() >= WarForgeConfig.LIVE_QUIT_TIMER) {
                    getAttacking().messageAll(Component.translatable("warforge.common.siege_camp.defenders_fled",
                            TimeHelper.formatTime(siege.getDefenderOfflineTimerMs())));
                    siege.setDefenderOfflineTimerMs(-1);  // negative => live quit
                    passSiege();
                    return;
                }
            } else {
                decrementOfflineTimer(siege, msPassed);
                if (attackersPresent && numActiveDefenders < 1) {
                    if (handleDesertion(siege, false, checkPerTick)) return;
                } else {
                    siege.setDefenderAbandonTicks(Math.max(0, siege.getDefenderAbandonTicks() - timerStep));
                }
            }

            previousTimestamp = WarClock.currTickTimestamp;
        }

        ++tickTimer;
        setChanged();
    }

    private boolean haveDefendersLiveQuit(Siege siege, Faction defenders) {
        int maxPlayersBeforeLiveQuit = WarForgeConfig.MAX_OFFLINE_PLAYER_COUNT_MINIMUM < 0
                ? -WarForgeConfig.MAX_OFFLINE_PLAYER_COUNT_MINIMUM
                : Math.max((int) (defenders.getMemberCount() * WarForgeConfig.MAX_OFFLINE_PLAYER_PERCENT), WarForgeConfig.MAX_OFFLINE_PLAYER_COUNT_MINIMUM);
        return siege.getLargestSeenDefenderCount() > maxPlayersBeforeLiveQuit && defenders.countOnlineMembers(null) < 1;
    }

    /** Past LIVE_QUIT_TIMER the timer is set to -1, so larger values never occur. */
    private static void incrementOfflineTimer(Siege siege, long msPassed) {
        long timer = siege.getDefenderOfflineTimerMs();
        siege.setDefenderOfflineTimerMs(timer + (timer < 0 ? -msPassed : msPassed));
    }

    /** Never overshoots 0. */
    private static void decrementOfflineTimer(Siege siege, long msPassed) {
        long timer = siege.getDefenderOfflineTimerMs();
        if (timer == 0) return;
        long newTimer = timer + (timer < 0 ? msPassed : -msPassed);
        siege.setDefenderOfflineTimerMs((timer < 0 && newTimer >= 0 || timer > 0 && newTimer <= 0) ? 0 : newTimer);
    }

    /** Returns true when the siege concluded and the tick must stop. */
    private boolean handleDesertion(Siege siege, boolean isAttackingSide, boolean checkPerTick) {
        int abandonTimer = isAttackingSide ? WarForgeConfig.ATTACKER_DESERTION_TIMER : WarForgeConfig.DEFENDER_DESERTION_TIMER;
        int currentTickTimer = isAttackingSide ? siege.getAttackerAbandonTicks() : siege.getDefenderAbandonTicks();

        if (currentTickTimer >= abandonTimer * 20) {
            WarForgeMod.FACTIONS.siegeService.notifyAbandoned(getAttacking(), defenders(), isAttackingSide);
            if (isAttackingSide) failSiege();
            else passSiege();
            return true;
        }

        int next = currentTickTimer + (checkPerTick ? 1 : 20);
        if (isAttackingSide) siege.setAttackerAbandonTicks(next);
        else siege.setDefenderAbandonTicks(next);
        return false;
    }

    private boolean isDefenderInWarzone(Player player) {
        return isPlayerInRadius(player, SIEGE_DEFENDER_RADIUS);
    }

    private boolean isPlayerInRadius(Player player, int radius) {
        if (player == null) return false;
        DimChunkPos playerChunk = new DimChunkPos(player.level().dimension(), player.blockPosition());
        DimChunkPos blockChunk = new DimChunkPos(level.dimension(), getClaimPos());
        return !player.isRemoved() && ChunkRadius.contains(blockChunk, playerChunk, radius);
    }

    private static void messageAllDefenders(Faction defenders, String translateKey, Object... args) {
        defenders.messageAll(Component.translatable(translateKey, args));
    }

    private Faction getAttacking() {
        return WarForgeMod.FACTIONS.registry.getFaction(factionUUID);
    }

    @Override
    protected void writeData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeData(nbt, registries);

        Nbt.putUUID(nbt, "placer", placer);
        nbt.putBoolean("started", siegeTarget != null);
        nbt.putInt("siegeStatus", siegeStatus.ordinal());
        nbt.putInt("tickTimer", tickTimer);
        if (siegeTarget != null) {
            siegeTarget.writeToNBT(nbt, "siegeTarget");
        }
    }

    @Override
    protected void readData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.readData(nbt, registries);

        // absent from the sync tag, which Flashback's replay server loads through readData
        placer = Nbt.hasUUID(nbt, "placer") ? Nbt.getUUID(nbt, "placer") : FactionIds.NONE;
        siegeStatus = SiegeStatus.VALUES[Mth.clamp(Nbt.getInt(nbt, "siegeStatus"), 0, SiegeStatus.VALUES.length - 1)];
        tickTimer = Nbt.getInt(nbt, "tickTimer");
        previousTimestamp = WarClock.currTickTimestamp;
        siegeTarget = Nbt.getBoolean(nbt, "started") ? DimBlockPos.readFromNBT(nbt, "siegeTarget") : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide() || siegeTarget == null) {
            return;
        }
        Faction defenders = getDefenders(siegeTarget);
        if (defenders == null) {
            WarForgeMod.LOGGER.warn("Siege camp at {} loaded with target {} but no defending faction; clearing target.", worldPosition, siegeTarget);
            siegeTarget = null;
            return;
        }
        defendersId = defenders.getUuid();
        lastSeenDefenderCount = defenders.countOnlineMembers(null);
    }
}
