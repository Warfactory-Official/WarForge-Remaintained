package com.flansmod.warforge.api.modularui;

import com.flansmod.warforge.api.ChunkDynamicTextureThread;
import com.flansmod.warforge.api.Color4i;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkMapTextureDaemon {
    private static final ConcurrentHashMap<String, AtomicInteger> GENERATIONS = new ConcurrentHashMap<String, AtomicInteger>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "WarForge-ChunkMapTextureDaemon");
            thread.setDaemon(true);
            return thread;
        }
    });

    public static void requestMapUpdate(String namespace, int dim, int centerX, int centerZ, int radius, Map<Long, Integer> tintByChunk) {
        final int generation = GENERATIONS.computeIfAbsent(namespace, ignored -> new AtomicInteger(0)).incrementAndGet();
        final HashMap<Long, Integer> tintCopy = new HashMap<Long, Integer>(tintByChunk);
        EXECUTOR.submit(() -> buildTextures(namespace, generation, dim, centerX, centerZ, radius, tintCopy));
    }

    public static void flushTextureQueue() {
        ChunkDynamicTextureThread.RegisterTextureAction action;
        int processed = 0;
        while ((action = ChunkDynamicTextureThread.queue.poll()) != null && processed < 64) {
            action.register();
            processed++;
        }
    }

    public static String getTextureName(String namespace, int dim, int x, int z) {
        return namespace + "/" + dim + "_" + x + "_" + z;
    }

    private static void buildTextures(String namespace, int generation, int dim, int centerX, int centerZ, int radius, Map<Long, Integer> tintByChunk) {
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = mc.world;
        if (world == null || world.provider.getDimension() != dim) {
            return;
        }

        HashMap<ChunkPos, Chunk> loadedChunks = new HashMap<ChunkPos, Chunk>();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int x = centerX - radius - 1; x <= centerX + radius + 1; x++) {
            for (int z = centerZ - radius - 1; z <= centerZ + radius + 1; z++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
                if (chunk == null) {
                    continue;
                }
                loadedChunks.put(chunk.getPos(), chunk);

                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        int y = chunk.getHeightValue(lx, lz);
                        if (y < minY) {
                            minY = y;
                        }
                        if (y > maxY) {
                            maxY = y;
                        }
                    }
                }
            }
        }

        if (minY == Integer.MAX_VALUE) {
            minY = 0;
            maxY = 1;
        }

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                AtomicInteger current = GENERATIONS.get(namespace);
                if (current == null || current.get() != generation) {
                    return;
                }

                int[] rawChunk17 = new int[17 * 17];
                int[] heightMap17 = new int[17 * 17];
                Integer tint = tintByChunk.get(ChunkMapUtil.key(x, z));

                for (int dz = -1; dz <= 15; dz++) {
                    for (int dx = -1; dx <= 15; dx++) {
                        int worldX = (x << 4) + dx;
                        int worldZ = (z << 4) + dz;
                        int neighborCX = worldX >> 4;
                        int neighborCZ = worldZ >> 4;
                        int localX = worldX & 15;
                        int localZ = worldZ & 15;
                        int index = (dx + 1) + (dz + 1) * 17;

                        Chunk neighbor = loadedChunks.get(new ChunkPos(neighborCX, neighborCZ));
                        if (neighbor != null) {
                            int y = neighbor.getHeightValue(localX, localZ);
                            heightMap17[index] = y;
                            IBlockState state = neighbor.getBlockState(localX, y - 1, localZ);
                            MapColor mapColor = state.getMapColor(world, new BlockPos(worldX, y - 1, worldZ));
                            rawChunk17[index] = 0xFF000000 | mapColor.colorValue;
                        } else if (tint != null) {
                            heightMap17[index] = minY;
                            rawChunk17[index] = 0xFF000000 | (tint & 0x00FFFFFF);
                        } else {
                            heightMap17[index] = minY;
                            rawChunk17[index] = 0xFF2A2A2A;
                        }
                    }
                }

                if (tint != null && tint != 0) {
                    int tintRgb = tint & 0x00FFFFFF;
                    for (int i = 0; i < rawChunk17.length; i++) {
                        Color4i base = Color4i.fromRGB(rawChunk17[i]);
                        Color4i fac = Color4i.fromRGB(tintRgb);
                        rawChunk17[i] = new Color4i(
                                255,
                                (base.getRed() * 7 + fac.getRed() * 3) / 10,
                                (base.getGreen() * 7 + fac.getGreen() * 3) / 10,
                                (base.getBlue() * 7 + fac.getBlue() * 3) / 10
                        ).toRGB();
                    }
                }

                new ChunkDynamicTextureThread(
                        4,
                        getTextureName(namespace, dim, x, z),
                        rawChunk17,
                        heightMap17,
                        maxY,
                        minY
                ).run();
            }
        }
    }
}
