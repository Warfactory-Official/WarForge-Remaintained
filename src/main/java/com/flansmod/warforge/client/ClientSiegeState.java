package com.flansmod.warforge.client;

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
        for (int i = 0; i < SIEGES.size(); i++) {
            if (SIEGES.get(i).attackingPos.equals(info.attackingPos)) {
                SIEGES.set(i, info);
                BorderSyncScheduler.invalidate();
                return;
            }
        }
        SIEGES.add(info);
        BorderSyncScheduler.invalidate();
    }

    public static void tick() {
        for (int i = SIEGES.size() - 1; i >= 0; i--) {
            SiegeCampProgressInfo siege = SIEGES.get(i);
            siege.clientTick();
            if (siege.isExpired()) {
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
