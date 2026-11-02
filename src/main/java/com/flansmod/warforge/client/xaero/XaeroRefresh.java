package com.flansmod.warforge.client.xaero;

import com.flansmod.warforge.client.ClientBorderCache;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.highlight.DimensionHighlighterHandler;
import xaero.common.minimap.write.MinimapWriter;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.world.MapDimension;

import java.util.concurrent.atomic.AtomicBoolean;

public final class XaeroRefresh {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean WORLD_MAP_LISTENING = new AtomicBoolean();
    private static final AtomicBoolean MINIMAP_LISTENING = new AtomicBoolean();

    private XaeroRefresh() {
    }

    public static void listenForWorldMap() {
        if (WORLD_MAP_LISTENING.compareAndSet(false, true)) {
            ClientBorderCache.addListener(new RegionListener() {
                @Override
                void refresh(ResourceKey<Level> dim, int regionX, int regionZ, boolean all) {
                    WorldMapSession session = WorldMapSession.getCurrentSession();
                    MapProcessor processor = session == null ? null : session.getMapProcessor();
                    if (processor == null || processor.getMapWorld() == null) {
                        return;
                    }
                    if (all) {
                        processor.getMapWorld().clearAllCachedHighlightHashes();
                        return;
                    }
                    MapDimension dimension = processor.getMapWorld().getDimension(dim);
                    if (dimension != null) {
                        dimension.getHighlightHandler().clearCachedHash(regionX, regionZ);
                    }
                }
            });
        }
    }

    public static void listenForMinimap() {
        if (MINIMAP_LISTENING.compareAndSet(false, true)) {
            ClientBorderCache.addListener(new RegionListener() {
                @Override
                void refresh(ResourceKey<Level> dim, int regionX, int regionZ, boolean all) {
                    XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
                    MinimapProcessor processor = session == null ? null : session.getMinimapProcessor();
                    MinimapWriter writer = processor == null ? null : processor.getMinimapWriter();
                    DimensionHighlighterHandler handler = writer == null ? null : writer.getDimensionHighlightHandler();
                    if (handler == null) {
                        return;
                    }
                    if (all) {
                        handler.requestRefresh();
                    } else {
                        handler.requestRefresh(regionX, regionZ);
                    }
                }
            });
        }
    }

    /** Errors logged, never reach the packet handler. */
    private abstract static class RegionListener implements ClientBorderCache.Listener {
        abstract void refresh(ResourceKey<Level> dim, int regionX, int regionZ, boolean all);

        @Override
        public void onClaimSet(ResourceKey<Level> dim, int x, int z, int colour) {
            refreshAround(dim, x, z);
        }

        @Override
        public void onClaimRemoved(ResourceKey<Level> dim, int x, int z) {
            refreshAround(dim, x, z);
        }

        @Override
        public void onCleared() {
            safely(() -> refresh(null, 0, 0, true));
        }

        private void refreshAround(ResourceKey<Level> dim, int x, int z) {
            safely(() -> {
                int regionX = x >> 5;
                int regionZ = z >> 5;
                refresh(dim, regionX, regionZ, false);
                refreshNeighbour(dim, regionX, regionZ, x + 1, z);
                refreshNeighbour(dim, regionX, regionZ, x - 1, z);
                refreshNeighbour(dim, regionX, regionZ, x, z + 1);
                refreshNeighbour(dim, regionX, regionZ, x, z - 1);
            });
        }

        private void refreshNeighbour(ResourceKey<Level> dim, int regionX, int regionZ, int neighbourX, int neighbourZ) {
            int nx = neighbourX >> 5;
            int nz = neighbourZ >> 5;
            if (nx != regionX || nz != regionZ) {
                refresh(dim, nx, nz, false);
            }
        }

        private static void safely(Runnable action) {
            try {
                action.run();
            } catch (Throwable t) {
                LOGGER.warn("Could not refresh the claims on a Xaero map", t);
            }
        }
    }
}
