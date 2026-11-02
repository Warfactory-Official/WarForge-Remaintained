package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketSiegeCampProgressUpdate;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Map;

/** Siege progress packets to nearby clients and faction members. */
public final class SiegeSync {
    private static final int HEARTBEAT_TICKS = 100;
    private static final double INFO_CENTER_Y = 128d;
    private static final float INFO_RANGE_PADDING = 128f;
    private static final int BLOCKS_PER_CHUNK = 16;

    private final FactionStorage hub;
    private int heartbeatTicks = 0;

    SiegeSync(FactionStorage hub) {
        this.hub = hub;
    }

    /** @return null if a faction or the anchor camp is missing (siege ended or being torn down) */
    public SiegeCampProgressInfo buildInfo(Siege siege) {
        Faction attackers = hub.registry.getFaction(siege.attackingFaction);
        Faction defenders = hub.registry.getFaction(siege.defendingFaction);
        if (attackers == null || defenders == null || siege.attackingCamps.isEmpty() || siege.attackingCamps.get(0) == null) {
            return null;
        }
        SiegeCampProgressInfo info = new SiegeCampProgressInfo();
        info.attackingPos = siege.attackingCamps.get(0);
        info.attackingName = attackers.getName();
        info.attackingColour = attackers.getColour();
        info.defendingPos = siege.defendingClaim;
        info.defendingName = defenders.getName();
        info.defendingColour = defenders.getColour();
        info.battleRadius = siege.getBattleRadius();
        info.siegedRadius = siege.getSiegedRadius();
        info.defendingFactionId = siege.defendingFaction;
        info.attackingFactionId = siege.attackingFaction;
        info.attackerAbandonSeconds = attackerAbandonSeconds(siege);
        info.progress = siege.getAttackProgress();
        info.completionPoint = siege.getAttackSuccessThreshold();
        info.timeProgress = siege.getTimeRemainingMillis();
        info.endTimestamp = siege.getSiegeEndTimeStamp();
        info.finished = siege.finished;
        return info;
    }

    /** Seconds until the attackers are considered to have deserted; 0 when not at risk. */
    private static int attackerAbandonSeconds(Siege siege) {
        int ticks = siege.getAttackerAbandonTicks();
        if (ticks <= 0 || (siege.campless && !WarForgeConfig.SIEGE_DECLARE_REQUIRE_PRESENCE)) {
            return 0;
        }
        return Math.max(0, WarForgeConfig.ATTACKER_DESERTION_TIMER - ticks / 20);
    }

    public void sendInfoToNearby(DimChunkPos siegePos) {
        Siege siege = hub.sieges.get(siegePos);
        if (siege == null) {
            return;
        }
        SiegeCampProgressInfo info = buildInfo(siege);
        if (info != null) {
            PacketSiegeCampProgressUpdate packet = new PacketSiegeCampProgressUpdate();
            packet.info = info;
            sendAround(packet, siegePos);
        }
    }

    public void sendAllInfoToNearby() {
        for (Map.Entry<DimChunkPos, Siege> kvp : hub.sieges.entries().entrySet()) {
            hub.siegeService.recalculateBasePower(kvp.getValue());
            sendInfoToNearby(kvp.getKey());
        }
    }

    /** Tells every client that could show the siege that it is over (nothing else prunes the HUD entry). */
    void broadcastEnded(Siege siege) {
        if (siege.defendingClaim == null) {
            return;
        }
        SiegeCampProgressInfo info = buildInfo(siege);
        if (info == null) {
            info = new SiegeCampProgressInfo();
            info.attackingPos = siege.attackingCamps.isEmpty() || siege.attackingCamps.get(0) == null
                    ? siege.defendingClaim
                    : siege.attackingCamps.get(0);
            info.defendingPos = siege.defendingClaim;
            info.attackingName = "";
            info.defendingName = "";
        }
        info.finished = true;
        info.attackerAbandonSeconds = 0;

        PacketSiegeCampProgressUpdate packet = new PacketSiegeCampProgressUpdate();
        packet.info = info;
        sendAround(packet, siege.defendingClaim.toChunkPos());
        sendToMembers(hub.registry.getFaction(siege.attackingFaction), packet);
        sendToMembers(hub.registry.getFaction(siege.defendingFaction), packet);
    }

    /** Re-sends info every {@link #HEARTBEAT_TICKS} ticks so clients that missed an update recover. */
    public void tickHeartbeat() {
        if (hub.sieges.isEmpty()) {
            heartbeatTicks = 0;
            return;
        }
        if (++heartbeatTicks < HEARTBEAT_TICKS) {
            return;
        }
        heartbeatTicks = 0;
        for (DimChunkPos siegePos : new ArrayList<>(hub.sieges.entries().keySet())) {
            sendInfoToNearby(siegePos);
        }
    }

    /** Client-side wipe of every remembered siege (identical attacker and defender name = clear). */
    public static PacketSiegeCampProgressUpdate clearSiegeData() {
        PacketSiegeCampProgressUpdate packet = new PacketSiegeCampProgressUpdate();
        packet.info = new SiegeCampProgressInfo();
        packet.info.expiredTicks = 0;
        packet.info.attackingName = "c";
        packet.info.defendingName = "c";
        packet.info.attackingPos = DimBlockPos.ZERO;
        packet.info.defendingPos = DimBlockPos.ZERO;
        return packet;
    }

    private static void sendAround(PacketSiegeCampProgressUpdate packet, DimChunkPos chunk) {
        WarForgeMod.NETWORK.sendToAllAround(packet, chunk.x * BLOCKS_PER_CHUNK, INFO_CENTER_Y, chunk.z * BLOCKS_PER_CHUNK,
                WarForgeConfig.SIEGE_INFO_RADIUS + INFO_RANGE_PADDING, chunk.dim);
    }

    private static void sendToMembers(Faction faction, PacketSiegeCampProgressUpdate packet) {
        if (faction == null) {
            return;
        }
        for (Player member : faction.getOnlinePlayers(p -> true)) {
            if (member instanceof ServerPlayer serverMember) {
                WarForgeMod.NETWORK.sendTo(packet, serverMember);
            }
        }
    }
}
