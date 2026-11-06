package com.flansmod.warforge.client;

import com.flansmod.warforge.client.render.HudClock;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class ClientSiegeState {
    private static final ObjectArrayList<SiegeCampProgressInfo> SIEGES = new ObjectArrayList<>();

    private ClientSiegeState() {
    }

    public static void clear() {
        SIEGES.clear();
    }

    public static void update(SiegeCampProgressInfo info) {
        info.receivedAtMs = HudClock.now();
        // SiegeSync.broadcastEnded's fallback (camp/faction already gone) has no names, thresholds or end time:
        // finish the entry the client already shows instead of drawing that placeholder.
        boolean placeholder = info.finished && (info.attackingName == null || info.attackingName.isEmpty());
        for (int i = 0; i < SIEGES.size(); i++) {
            if (SIEGES.get(i).attackingPos.equals(info.attackingPos)) {
                if (placeholder) {
                    SIEGES.get(i).finished = true;
                    SIEGES.get(i).receivedAtMs = info.receivedAtMs;
                    return;
                }
                SIEGES.set(i, info);
                BorderSyncScheduler.invalidate();
                return;
            }
        }
        if (!placeholder) {
            SIEGES.add(info);
        }
        BorderSyncScheduler.invalidate();
    }

    public static void tick() {
        boolean replay = HudClock.replay();
        long now = HudClock.now();
        for (int i = SIEGES.size() - 1; i >= 0; i--) {
            SiegeCampProgressInfo siege = SIEGES.get(i);
            boolean expired;
            if (replay) {
                // Replay time pauses and seeks: age by it; seeking back before the info arrived drops it.
                long age = now - siege.receivedAtMs;
                long life = (siege.finished ? SiegeCampProgressInfo.FINISHED_LINGER_TICKS : SiegeCampProgressInfo.STALE_TICKS) * 50L;
                expired = age < 0 || age >= life;
            } else {
                siege.clientTick();
                expired = siege.isExpired();
            }
            if (expired) {
                SIEGES.remove(i);
            }
        }
    }

    public static SiegeCampProgressInfo closest(LocalPlayer player) {
        ResourceKey<Level> dim = player.level().dimension();
        BlockPos playerPos = player.blockPosition();
        double radiusSq = (double) WarForgeConfig.SIEGE_INFO_RADIUS * WarForgeConfig.SIEGE_INFO_RADIUS;
        SiegeCampProgressInfo closest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < SIEGES.size(); i++) {
            SiegeCampProgressInfo siege = SIEGES.get(i);
            if (!siege.defendingPos.dim.equals(dim)) {
                continue;
            }
            double distSq = siege.defendingPos.distSqr(playerPos);
            if (distSq < radiusSq && distSq < bestDistSq) {
                bestDistSq = distSq;
                closest = siege;
            }
        }
        return closest;
    }

    public static boolean inBattleZone(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        UUID playerFaction = ClientClaimChunkCache.playerFactionId();
        for (int i = 0; i < SIEGES.size(); i++) {
            SiegeCampProgressInfo siege = SIEGES.get(i);
            if (!siege.attackingPos.dim.equals(dim)
                    || !playerFaction.equals(siege.defendingFactionId) && !playerFaction.equals(siege.attackingFactionId)) {
                continue;
            }
            if (Math.abs(chunkX - (siege.attackingPos.getX() >> 4)) <= siege.battleRadius
                    && Math.abs(chunkZ - (siege.attackingPos.getZ() >> 4)) <= siege.battleRadius) {
                return true;
            }
        }
        return false;
    }
}
