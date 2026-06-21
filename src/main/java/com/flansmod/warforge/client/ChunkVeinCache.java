package com.flansmod.warforge.client;

import org.apache.commons.lang3.tuple.Pair;
import com.flansmod.warforge.api.WarforgeCache;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.init.VeinUtils;
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
        cache.put(chunkPos, VeinUtils.decompressVeinInfo(compressedVeinInfo));
    }

    public Pair<Vein, Quality> get(DimChunkPos chunkPos) {
        return cache.get(chunkPos);
    }

    // checks if a packet is received and recognized
    public boolean hasValidData(DimChunkPos chunkPosKey) {
        return isReceived(chunkPosKey) && isRecognized(chunkPosKey);
    }

    // checks if a packet for the position was ever received and processed
    public boolean isReceived(DimChunkPos chunkPosKey) {
        return cache.contains(chunkPosKey);
    }

    // checks that the pos is recognized, not checking if it has already been received
    public boolean isRecognized(DimChunkPos chunkPosKey) {
        Pair<Vein, Quality> veinInfo = cache.get(chunkPosKey);
        return veinInfo == null || veinInfo.getLeft() != null;
    }
}
