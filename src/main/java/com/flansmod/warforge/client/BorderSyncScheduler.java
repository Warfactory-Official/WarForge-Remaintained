package com.flansmod.warforge.client;

import com.flansmod.warforge.client.ui.model.ClaimMapModel;
import com.flansmod.warforge.client.ui.ClaimManagerScreen;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRequestClaimChunks;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Outline window re-requested only on drift from last centre, radius change, sieges update or safety interval. */
public final class BorderSyncScheduler {
    private static final int PREFETCH_CHUNKS = 4;
    private static final int SAFETY_RESYNC_TICKS = 100;
    private static final int CLAIM_MANAGER_REFRESH_TICKS = 40;

    private static ResourceKey<Level> syncDim;
    private static int syncX;
    private static int syncZ;
    private static int syncRadius;
    private static boolean synced;

    private static ResourceKey<Level> standingDim;
    private static int standingX;
    private static int standingZ;

    private BorderSyncScheduler() {
    }

    public static void invalidate() {
        synced = false;
    }

    public static void invalidateNear(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        if (synced && dim.equals(syncDim) && Math.abs(chunkX - syncX) <= syncRadius && Math.abs(chunkZ - syncZ) <= syncRadius) {
            synced = false;
        }
    }

    public static void reset() {
        synced = false;
        syncDim = null;
        standingDim = null;
    }

    public static int renderDistanceChunks(Minecraft mc) {
        return ClientConfig.BORDER_RENDER_DISTANCE > 0
                ? ClientConfig.BORDER_RENDER_DISTANCE
                : mc.options.getEffectiveRenderDistance();
    }

    public static void tick(Minecraft mc, LocalPlayer player, ResourceKey<Level> dim, int chunkX, int chunkZ, boolean screenOpen) {
        if (!screenOpen) {
            int radius = Math.min(renderDistanceChunks(mc) + PREFETCH_CHUNKS, WarForgeConfig.BORDER_SYNC_MAX_RADIUS);
            boolean resync = !synced
                    || !syncDim.equals(dim)
                    || radius != syncRadius
                    || Math.max(Math.abs(chunkX - syncX), Math.abs(chunkZ - syncZ)) > PREFETCH_CHUNKS
                    || player.tickCount % SAFETY_RESYNC_TICKS == 0;
            if (resync) {
                request(dim, chunkX, chunkZ, radius, true);
                syncDim = dim;
                syncX = chunkX;
                syncZ = chunkZ;
                syncRadius = radius;
                synced = true;
            }
            standingDim = dim;
            standingX = chunkX;
            standingZ = chunkZ;
        } else if (player.tickCount % CLAIM_MANAGER_REFRESH_TICKS == 0
                && ClaimManagerScreen.isCurrentlyOpen()
                && dim.equals(standingDim)
                && !ClaimMapModel.isRemoteSiegeView()) {
            request(standingDim, standingX, standingZ, WarForgeConfig.CLAIM_MANAGER_RADIUS, false);
            synced = false;
        }
    }

    private static void request(ResourceKey<Level> dim, int chunkX, int chunkZ, int radius, boolean outlineOnly) {
        PacketRequestClaimChunks packet = new PacketRequestClaimChunks();
        packet.center = new DimChunkPos(dim, chunkX, chunkZ);
        packet.radius = radius;
        packet.outlineOnly = outlineOnly;
        WarForgeMod.NETWORK.sendToServer(packet);
    }
}
