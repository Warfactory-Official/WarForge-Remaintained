package com.flansmod.warforge.client;

import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side store of the chunk veins this client is allowed to see, fed by {@code PacketJourneyMapVeins}.
 * <p>
 * Free of any JourneyMap references so it loads with or without JourneyMap. The optional JourneyMap vein
 * plugin registers itself as the {@link Listener} and turns these updates into icon markers; if JourneyMap
 * is absent the data simply sits inert. The payload is the compressed vein id/quality short only — the
 * icon is resolved client-side from the already-synced vein definitions.
 */
public final class JourneyMapVeinCache {
    public interface Listener {
        void onVeinSet(ResourceKey<Level> dim, int x, int z, short veinInfo);

        void onVeinRemoved(ResourceKey<Level> dim, int x, int z);

        void onCleared();
    }

    private static final Map<DimChunkPos, Short> VEINS = new HashMap<>();
    private static Listener listener;

    private JourneyMapVeinCache() {
    }

    public static synchronized void setListener(Listener newListener) {
        listener = newListener;
        if (newListener != null) {
            for (Map.Entry<DimChunkPos, Short> entry : VEINS.entrySet()) {
                DimChunkPos pos = entry.getKey();
                newListener.onVeinSet(pos.dim, pos.x, pos.z, entry.getValue());
            }
        }
    }

    public static synchronized void applyClear() {
        VEINS.clear();
        if (listener != null) {
            listener.onCleared();
        }
    }

    public static synchronized void set(ResourceKey<Level> dim, int x, int z, short veinInfo) {
        VEINS.put(new DimChunkPos(dim, x, z), veinInfo);
        if (listener != null) {
            listener.onVeinSet(dim, x, z, veinInfo);
        }
    }

    public static synchronized void remove(ResourceKey<Level> dim, int x, int z) {
        if (VEINS.remove(new DimChunkPos(dim, x, z)) != null && listener != null) {
            listener.onVeinRemoved(dim, x, z);
        }
    }
}
