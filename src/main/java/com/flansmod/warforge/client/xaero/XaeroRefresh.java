package com.flansmod.warforge.client.xaero;

import com.flansmod.warforge.client.MinimapClaimCache;
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

/**
 * Tells Xaero's maps to redraw the regions whose claims changed. There is one listener per map mod; each is added the
 * first time that mod builds its highlighter registry, so neither loads when its mod is absent.
 */
public final class XaeroRefresh {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean WORLD_MAP_LISTENING = new AtomicBoolean();
    private static final AtomicBoolean MINIMAP_LISTENING = new AtomicBoolean();

    private XaeroRefresh() {
    }

    /** Called when Xaero's World Map builds its highlighter registry. */
    public static void listenForWorldMap() {
        if (WORLD_MAP_LISTENING.compareAndSet(false, true)) {
            MinimapClaimCache.addListener(new RegionListener() {
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

    /** Called when Xaero's Minimap builds its highlighter registry. */
    public static void listenForMinimap() {
        if (MINIMAP_LISTENING.compareAndSet(false, true)) {
            MinimapClaimCache.addListener(new RegionListener() {
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

    /**
     * Turns claim changes into region refreshes: the chunk's region and the regions of its four neighbours (a
     * neighbour's border depends on this chunk). Errors are logged and never reach the packet handler.
     */
    private abstract static class RegionListener implements MinimapClaimCache.Listener {
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
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = (x + d[0]) >> 5;
                    int nz = (z + d[1]) >> 5;
                    if (nx != regionX || nz != regionZ) {
                        refresh(dim, nx, nz, false);
                    }
                }
            });
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
