package com.flansmod.warforge.client;

import com.flansmod.warforge.common.SyncedVeins;
import org.apache.commons.lang3.tuple.Pair;
import com.flansmod.warforge.api.WarforgeCache;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.VeinCodec;
import com.flansmod.warforge.common.util.DimChunkPos;

public class ChunkVeinCache {
    protected WarforgeCache<DimChunkPos, Pair<Vein, Quality>> cache;

    public ChunkVeinCache() {
        cache = new WarforgeCache<>(0, 64);
    }

    public void purge(){
        cache.clear();
    }

    public void add(DimChunkPos chunkPos, short compressedVeinInfo) {
        cache.put(chunkPos, decompress(compressedVeinInfo));
    }

    /** Null for the null vein; a pair of nulls for a vein the client has no entry for. */
    public static Pair<Vein, Quality> decompress(short veinInfo) {
        if (veinInfo == VeinCodec.NULL_VEIN_ID) { return null; }
        short[] split = VeinCodec.splitVeinInfo(veinInfo);
        Vein vein = SyncedVeins.ENTRIES.get(split[0]);
        return vein == null ? Pair.of(null, null) : Pair.of(vein, Quality.getQuality(split[1]));
    }

    public Pair<Vein, Quality> get(DimChunkPos chunkPos) {
        return cache.get(chunkPos);
    }

    public boolean isReceived(DimChunkPos chunkPosKey) {
        return cache.contains(chunkPosKey);
    }

    public boolean isRecognized(DimChunkPos chunkPosKey) {
        Pair<Vein, Quality> veinInfo = cache.get(chunkPosKey);
        return veinInfo == null || veinInfo.getLeft() != null;
    }
}
