package com.flansmod.warforge.client;

import com.flansmod.warforge.common.util.DimChunkPos;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** No JourneyMap types: loads without the mod. */
public final class JourneyMapVeinCache {
    public interface Listener {
        void onVeinSet(ResourceKey<Level> dim, int x, int z, short veinInfo);

        void onVeinRemoved(ResourceKey<Level> dim, int x, int z);

        void onCleared();
    }

    private static final Object2ShortOpenHashMap<DimChunkPos> VEINS = new Object2ShortOpenHashMap<>();
    private static Listener listener;

    private JourneyMapVeinCache() {
    }

    public static synchronized void setListener(Listener newListener) {
        listener = newListener;
        if (newListener != null) {
            for (Object2ShortMap.Entry<DimChunkPos> entry : VEINS.object2ShortEntrySet()) {
                DimChunkPos pos = entry.getKey();
                newListener.onVeinSet(pos.dim, pos.x, pos.z, entry.getShortValue());
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
        DimChunkPos key = new DimChunkPos(dim, x, z);
        boolean known = VEINS.containsKey(key);
        VEINS.removeShort(key);
        if (known && listener != null) {
            listener.onVeinRemoved(dim, x, z);
        }
    }
}
