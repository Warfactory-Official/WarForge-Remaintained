package com.flansmod.warforge.client;

import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side store of the claim borders this client is allowed to see, fed by {@code PacketJourneyMapClaims}.
 * <p>
 * Deliberately free of any map mod references so it loads with or without JourneyMap or Xaero's maps (and on a
 * dedicated server, where it is never actually populated). The optional map integrations either register as a
 * {@link Listener} and turn these updates into map overlays (JourneyMap), or read {@link #colourAt} when they draw
 * (Xaero's maps, which may read from their own threads). If no map mod is present the data sits inert.
 * <p>
 * The payload is intentionally minimal — chunk position plus faction color only, never faction names,
 * ids or membership — so it leaks no more association than a colored border requires.
 */
public final class MinimapClaimCache {
    public interface Listener {
        void onClaimSet(ResourceKey<Level> dim, int x, int z, int colour);

        void onClaimRemoved(ResourceKey<Level> dim, int x, int z);

        void onCleared();
    }

    private static final Map<DimChunkPos, Integer> CLAIMS = new ConcurrentHashMap<>();
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    /** Incremented on every change, so readers can tell when their cached view is stale. */
    private static volatile int revision;

    private MinimapClaimCache() {
    }

    public static synchronized void addListener(Listener listener) {
        LISTENERS.add(listener);
        // Replay current state so a plugin that initializes after data has arrived catches up.
        for (Map.Entry<DimChunkPos, Integer> entry : CLAIMS.entrySet()) {
            DimChunkPos pos = entry.getKey();
            listener.onClaimSet(pos.dim, pos.x, pos.z, entry.getValue());
        }
    }

    public static synchronized void applyClear() {
        CLAIMS.clear();
        revision++;
        for (Listener listener : LISTENERS) {
            listener.onCleared();
        }
    }

    public static synchronized void set(ResourceKey<Level> dim, int x, int z, int colour) {
        CLAIMS.put(new DimChunkPos(dim, x, z), colour);
        revision++;
        for (Listener listener : LISTENERS) {
            listener.onClaimSet(dim, x, z, colour);
        }
    }

    public static synchronized void remove(ResourceKey<Level> dim, int x, int z) {
        if (CLAIMS.remove(new DimChunkPos(dim, x, z)) != null) {
            revision++;
            for (Listener listener : LISTENERS) {
                listener.onClaimRemoved(dim, x, z);
            }
        }
    }

    /** The claim colour (RGB) of a chunk, or null if it is not claimed. Safe to call from any thread. */
    public static Integer colourAt(ResourceKey<Level> dim, int x, int z) {
        return CLAIMS.get(new DimChunkPos(dim, x, z));
    }

    /** A number that changes whenever a claim is set, removed or cleared. */
    public static int revision() {
        return revision;
    }
}
