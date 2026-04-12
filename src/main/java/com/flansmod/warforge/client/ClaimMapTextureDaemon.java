package com.flansmod.warforge.client;

import com.flansmod.warforge.api.ChunkDynamicTextureThread;
import com.flansmod.warforge.api.Color4i;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.network.PacketClaimChunksData;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ClaimMapTextureDaemon {
    private static final AtomicInteger GENERATION = new AtomicInteger(0);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "WarForge-ClaimMapTextureDaemon");
            thread.setDaemon(true);
            return thread;
        }
    });

    public static void requestMapUpdate(PacketClaimChunksData data) {
        final int generation = GENERATION.incrementAndGet();
        final int dim = data.dim;
        final int centerX = data.centerX;
        final int centerZ = data.centerZ;
        final int radius = data.radius;
        final HashMap<Long, ClaimChunkInfo> claimInfo = new HashMap<Long, ClaimChunkInfo>();
        for (ClaimChunkInfo info : data.chunks) {
            claimInfo.put(key(info.x, info.z), info);
        }

        EXECUTOR.submit(() -> buildTextures(generation, dim, centerX, centerZ, radius, claimInfo));
    }

    public static void flushTextureQueue() {
        ChunkDynamicTextureThread.RegisterTextureAction action;
        int processed = 0;
        while ((action = ChunkDynamicTextureThread.queue.poll()) != null && processed < 64) {
            action.register();
            processed++;
        }
    }

    public static String getTextureName(int dim, int x, int z) {
        return "claimmap/" + dim + "_" + x + "_" + z;
    }

    private static void buildTextures(int generation, int dim, int centerX, int centerZ, int radius, Map<Long, ClaimChunkInfo> infoByChunk) {
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
                ChunkPos pos = chunk.getPos();
                loadedChunks.put(pos, chunk);

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
                if (GENERATION.get() != generation) {
                    return;
                }

                int[] rawChunk17 = new int[17 * 17];
                int[] heightMap17 = new int[17 * 17];
                ClaimChunkInfo chunkInfo = infoByChunk.get(key(x, z));

                for (int dz = -1; dz <= 15; dz++) {
                    for (int dx = -1; dx <= 15; dx++) {
                        int worldX = (x << 4) + dx;
                        int worldZ = (z << 4) + dz;
                        int neighborCX = worldX >> 4;
                        int neighborCZ = worldZ >> 4;
                        int localX = worldX & 15;
                        int localZ = worldZ & 15;

                        Chunk neighbor = loadedChunks.get(new ChunkPos(neighborCX, neighborCZ));
                        int index = (dx + 1) + (dz + 1) * 17;

                        if (neighbor != null) {
                            int y = neighbor.getHeightValue(localX, localZ);
                            heightMap17[index] = y;
                            IBlockState state = neighbor.getBlockState(localX, y - 1, localZ);
                            MapColor mapColor = state.getMapColor(world, new BlockPos(worldX, y - 1, worldZ));
                            rawChunk17[index] = 0xFF000000 | mapColor.colorValue;
                        } else if (chunkInfo != null) {
                            heightMap17[index] = minY;
                            rawChunk17[index] = 0xFF000000 | (chunkInfo.colour & 0x00FFFFFF);
                        } else {
                            heightMap17[index] = minY;
                            rawChunk17[index] = 0xFF2A2A2A;
                        }
                    }
                }

                // Slightly tint owned chunk to match claim coloring, keeping terrain readability.
                if (chunkInfo != null && chunkInfo.colour != 0 && !chunkInfo.factionName.isEmpty()) {
                    int tint = chunkInfo.colour & 0x00FFFFFF;
                    for (int i = 0; i < rawChunk17.length; i++) {
                        Color4i base = Color4i.fromRGB(rawChunk17[i]);
                        Color4i fac = Color4i.fromRGB(tint);
                        int mixed = new Color4i(
                                255,
                                (base.getRed() * 7 + fac.getRed() * 3) / 10,
                                (base.getGreen() * 7 + fac.getGreen() * 3) / 10,
                                (base.getBlue() * 7 + fac.getBlue() * 3) / 10
                        ).toRGB();
                        rawChunk17[i] = mixed;
                    }
                }

                String textureName = getTextureName(dim, x, z);
                new ChunkDynamicTextureThread(4, textureName, rawChunk17, heightMap17, maxY, minY).run();
            }
        }
    }

    private static long key(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFF_FFFFL);
    }
}
