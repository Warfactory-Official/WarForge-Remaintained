package com.flansmod.warforge.client;

import com.flansmod.warforge.common.util.DimChunkPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side store of the claim borders this client is allowed to see, fed by {@code PacketJourneyMapClaims}.
 * <p>
 * Deliberately free of any JourneyMap references so it loads with or without JourneyMap (and on a dedicated
 * server, where it is never actually populated). The optional JourneyMap plugin registers itself as the
 * {@link Listener} and turns these updates into map overlays; if JourneyMap is absent there is simply no
 * listener and the data sits inert.
 * <p>
 * The payload is intentionally minimal — chunk position plus faction color only, never faction names,
 * ids or membership — so it leaks no more association than a colored border requires.
 */
public final class JourneyMapClaimCache {
    public interface Listener {
        void onClaimSet(int dim, int x, int z, int colour);

        void onClaimRemoved(int dim, int x, int z);

        void onCleared();
    }

    private static final Map<DimChunkPos, Integer> CLAIMS = new HashMap<>();
    private static Listener listener;

    private JourneyMapClaimCache() {
    }

    public static synchronized void setListener(Listener newListener) {
        listener = newListener;
        // Replay current state so a plugin that initializes after data has arrived catches up.
        if (newListener != null) {
            for (Map.Entry<DimChunkPos, Integer> entry : CLAIMS.entrySet()) {
                DimChunkPos pos = entry.getKey();
                newListener.onClaimSet(pos.dim, pos.x, pos.z, entry.getValue());
            }
        }
    }

    public static synchronized void applyClear() {
        CLAIMS.clear();
        if (listener != null) {
            listener.onCleared();
        }
    }

    public static synchronized void set(int dim, int x, int z, int colour) {
        CLAIMS.put(new DimChunkPos(dim, x, z), colour);
        if (listener != null) {
            listener.onClaimSet(dim, x, z, colour);
        }
    }

    public static synchronized void remove(int dim, int x, int z) {
        if (CLAIMS.remove(new DimChunkPos(dim, x, z)) != null && listener != null) {
            listener.onClaimRemoved(dim, x, z);
        }
    }
}
