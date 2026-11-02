package com.flansmod.warforge.client.map;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.ServerTerrainCache;
import com.flansmod.warforge.client.render.DynamicTextures;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Render thread only except the executor task, which reads the snapshot only. */
public final class ChunkMapTextureDaemon {
    private static final int SCALE = 4;
    private static final long SNAPSHOT_BUDGET_NANOS = 2_000_000L;
    private static final int UNKNOWN = Integer.MIN_VALUE;
    private static final int NO_COLOR = 0;

    private static final Map<String, Job> JOBS = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> ACTIVE_TEXTURES = new HashMap<>();
    private static final Map<String, MapRequest> LAST_REQUEST = new HashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "WarForge-ChunkMapTextureDaemon");
        thread.setDaemon(true);
        return thread;
    });

    private ChunkMapTextureDaemon() {}

    public static void requestMapUpdate(String namespace, ResourceKey<Level> dim, int centerX, int centerZ, int radius, Long2IntMap tintByChunk) {
        MapRequest request = new MapRequest(dim, centerX, centerZ, radius, new Long2IntOpenHashMap(tintByChunk));
        if (request.equals(LAST_REQUEST.get(namespace))) {
            return;
        }
        LAST_REQUEST.put(namespace, request);

        HashSet<String> desired = new HashSet<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                desired.add(getTextureName(namespace, dim, x, z));
            }
        }
        replaceActiveTextures(namespace, desired);
        retire(JOBS.put(namespace, new Job(namespace, request)));
    }

    /** Rebuilds the last request, bypassing the dedupe: new server terrain data does not change the request key. */
    public static void rebuildLast(String namespace) {
        MapRequest request = LAST_REQUEST.remove(namespace);
        if (request != null) {
            requestMapUpdate(namespace, request.dim(), request.centerX(), request.centerZ(), request.radius(), request.tints());
        }
    }

    public static void flushTextureQueue() {
        if (!JOBS.isEmpty()) {
            advanceSnapshots();
        }
        if (ChunkTextureBuilder.PENDING.isEmpty()) {
            return;
        }
        Iterator<ChunkTextureBuilder.RegisterTextureAction> it = ChunkTextureBuilder.PENDING.values().iterator();
        while (it.hasNext()) {
            ChunkTextureBuilder.RegisterTextureAction action = it.next();
            it.remove();
            action.register();
        }
    }

    public static String getTextureName(String namespace, ResourceKey<Level> dim, int x, int z) {
        return namespace + "/" + Keys.id(dim).getPath() + "_" + x + "_" + z;
    }

    public static void releaseNamespace(String namespace) {
        LAST_REQUEST.remove(namespace);
        retire(JOBS.remove(namespace));
        Set<String> active = ACTIVE_TEXTURES.remove(namespace);
        if (active == null) {
            return;
        }
        for (String name : active) {
            ChunkTextureBuilder.PENDING.remove(name);
            DynamicTextures.release(Ids.of(Tags.MODID, name));
        }
    }

    public static void releaseAll() {
        for (String namespace : new HashSet<>(ACTIVE_TEXTURES.keySet())) {
            releaseNamespace(namespace);
        }
        LAST_REQUEST.clear();
    }

    private static void retire(Job job) {
        if (job != null) {
            job.stale = true;
        }
    }

    private static void replaceActiveTextures(String namespace, Set<String> desired) {
        Set<String> current = ACTIVE_TEXTURES.get(namespace);
        if (current != null) {
            for (String old : current) {
                if (!desired.contains(old)) {
                    ChunkTextureBuilder.PENDING.remove(old);
                    DynamicTextures.release(Ids.of(Tags.MODID, old));
                }
            }
        }
        ACTIVE_TEXTURES.put(namespace, desired);
    }

    private static void advanceSnapshots() {
        ClientLevel level = Minecraft.getInstance().level;
        long deadline = System.nanoTime() + SNAPSHOT_BUDGET_NANOS;
        Iterator<Job> it = JOBS.values().iterator();
        while (it.hasNext()) {
            Job job = it.next();
            if (job.sampled) {
                continue;
            }
            if (level == null || level.dimension() != job.request.dim()) {
                retire(job);
                it.remove();
                continue;
            }
            if (job.sample(level, deadline)) {
                job.sampled = true;
                EXECUTOR.execute(() -> buildTextures(job));
            }
            if (System.nanoTime() >= deadline) {
                return;
            }
        }
    }

    private static void buildTextures(Job job) {
        try {
            buildAll(job);
        } finally {
            JOBS.remove(job.namespace, job);
        }
    }

    private static void buildAll(Job job) {
        MapRequest request = job.request;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int height : job.heights) {
            if (height != UNKNOWN) {
                minY = Math.min(minY, height);
                maxY = Math.max(maxY, height);
            }
        }
        if (minY == Integer.MAX_VALUE) {
            minY = 0;
            maxY = 1;
        }

        for (int x = request.centerX() - request.radius(); x <= request.centerX() + request.radius(); x++) {
            for (int z = request.centerZ() - request.radius(); z <= request.centerZ() + request.radius(); z++) {
                if (job.stale) {
                    return;
                }
                long chunkKey = ChunkMapUtil.key(x, z);
                boolean hasTint = request.tints().containsKey(chunkKey);
                int tint = request.tints().get(chunkKey);
                int[] rawChunk17 = new int[17 * 17];
                int[] heightMap17 = new int[17 * 17];
                job.extract(x, z, hasTint, tint, minY, rawChunk17, heightMap17);
                new ChunkTextureBuilder(SCALE, getTextureName(job.namespace, request.dim(), x, z),
                        rawChunk17, heightMap17, maxY, minY).build();
            }
        }
    }

    private static final class Job {
        final String namespace;
        final MapRequest request;
        final int originX;
        final int originZ;
        final int side;
        final int[] colors;
        final int[] heights;
        int nextRow;
        volatile boolean stale;
        volatile boolean sampled;

        Job(String namespace, MapRequest request) {
            this.namespace = namespace;
            this.request = request;
            this.originX = ((request.centerX() - request.radius()) << 4) - 1;
            this.originZ = ((request.centerZ() - request.radius()) << 4) - 1;
            this.side = (2 * request.radius() + 1) * 16 + 2;
            this.colors = new int[side * side];
            this.heights = new int[side * side];
        }

        boolean sample(ClientLevel level, long deadline) {
            ResourceKey<Level> dim = request.dim();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            LevelChunk chunk = null;
            int chunkX = Integer.MIN_VALUE;
            int chunkZ = Integer.MIN_VALUE;
            while (nextRow < side) {
                int worldZ = originZ + nextRow;
                int rowBase = nextRow * side;
                for (int col = 0; col < side; col++) {
                    int worldX = originX + col;
                    int cx = worldX >> 4;
                    int cz = worldZ >> 4;
                    if (cx != chunkX || cz != chunkZ) {
                        chunkX = cx;
                        chunkZ = cz;
                        chunk = level.getChunkSource().getChunkNow(cx, cz);
                    }
                    if (chunk != null) {
                        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, worldX & 15, worldZ & 15);
                        pos.set(worldX, y, worldZ);
                        heights[rowBase + col] = y;
                        colors[rowBase + col] = 0xFF000000 | MapBlockColorSampler.sampleColor(level, chunk.getBlockState(pos), pos);
                        continue;
                    }
                    int serverColor = ServerTerrainCache.colorAt(dim, worldX, worldZ);
                    if (serverColor == UNKNOWN) {
                        heights[rowBase + col] = UNKNOWN;
                        colors[rowBase + col] = NO_COLOR;
                    } else {
                        heights[rowBase + col] = ServerTerrainCache.heightAt(dim, worldX, worldZ);
                        colors[rowBase + col] = 0xFF000000 | serverColor;
                    }
                }
                nextRow++;
                if (System.nanoTime() >= deadline) {
                    break;
                }
            }
            return nextRow >= side;
        }

        void extract(int chunkX, int chunkZ, boolean hasTint, int tint, int fallbackHeight, int[] rawChunk17, int[] heightMap17) {
            int tintRgb = tint & 0x00FFFFFF;
            for (int dz = 0; dz < 17; dz++) {
                int src = (((chunkZ << 4) - 1 + dz) - originZ) * side + (((chunkX << 4) - 1) - originX);
                for (int dx = 0; dx < 17; dx++) {
                    int index = dx + dz * 17;
                    int color = colors[src + dx];
                    int height = heights[src + dx];
                    if (color == NO_COLOR) {
                        rawChunk17[index] = hasTint ? 0xFF000000 | tintRgb : 0xFF2A2A2A;
                        heightMap17[index] = fallbackHeight;
                    } else {
                        rawChunk17[index] = color;
                        heightMap17[index] = height != UNKNOWN ? height : fallbackHeight;
                    }
                }
            }
            if (hasTint && tint != 0) {
                int tr = (tintRgb >> 16) & 0xFF;
                int tg = (tintRgb >> 8) & 0xFF;
                int tb = tintRgb & 0xFF;
                for (int i = 0; i < rawChunk17.length; i++) {
                    int c = rawChunk17[i];
                    int r = (((c >> 16) & 0xFF) * 7 + tr * 3) / 10;
                    int g = (((c >> 8) & 0xFF) * 7 + tg * 3) / 10;
                    int b = ((c & 0xFF) * 7 + tb * 3) / 10;
                    rawChunk17[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
    }

    private record MapRequest(ResourceKey<Level> dim, int centerX, int centerZ, int radius, Long2IntOpenHashMap tints) {}
}
