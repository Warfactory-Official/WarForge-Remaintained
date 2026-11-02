package com.flansmod.warforge.common.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;

public final class PacketSecurity {
    private PacketSecurity() {
    }

    public static final int MAX_RESOURCE_LOCATION = 256;

    // >= WarForgeConfig max faction name length ceiling
    public static final int MAX_NAME = 128;
    public static final int MAX_FLAG_ID = 128;
    public static final int MAX_FOB_NAME = 32;

    // vanilla reach 4.5 (6 creative) + latency slack
    public static final double MAX_BLOCK_INTERACT_DIST = 12.0D;
    private static final double MAX_BLOCK_INTERACT_DIST_SQ = MAX_BLOCK_INTERACT_DIST * MAX_BLOCK_INTERACT_DIST;

    /** Burst capacity / sustained refill per second. */
    public enum Bucket {
        QUERY(150, 50),
        // (2r+1)^2 chunk lookups per request, up to 9409 at border-sync radius
        MAP(12, 4),
        TERRAIN(8, 3),
        ACTION(20, 8),
        NONE(0, 0);

        final int capacity;
        final int refillPerSecond;

        Bucket(int capacity, int refillPerSecond) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
        }
    }

    private static final class Buckets {
        final double[] tokens = new double[Bucket.values().length];
        final long[] lastRefillMs = new long[Bucket.values().length];
        long lastLogMs = 0L;
        int suppressedLogs = 0;

        Buckets(long now) {
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = Bucket.values()[i].capacity;
                lastRefillMs[i] = now;
            }
        }
    }

    private static final Map<UUID, Buckets> BUCKETS = new ConcurrentHashMap<>();

    private static final long VIOLATION_LOG_INTERVAL_MS = 5_000L;

    public static boolean allow(ServerPlayer player, Bucket bucket, String packetName) {
        if (bucket == Bucket.NONE || player == null) {
            return true;
        }

        long now = System.currentTimeMillis();
        Buckets state = BUCKETS.computeIfAbsent(player.getUUID(), id -> new Buckets(now));
        int idx = bucket.ordinal();

        synchronized (state) {
            long elapsed = Math.max(0L, now - state.lastRefillMs[idx]);
            state.lastRefillMs[idx] = now;
            state.tokens[idx] = Math.min(bucket.capacity,
                    state.tokens[idx] + (elapsed / 1000.0D) * bucket.refillPerSecond);

            if (state.tokens[idx] < 1.0D) {
                logThrottled(player, state, now, "rate limit exceeded on " + packetName + " (" + bucket + ")");
                return false;
            }
            state.tokens[idx] -= 1.0D;
        }
        return true;
    }

    public static void violation(ServerPlayer player, String detail) {
        if (player == null) {
            WarForgeMod.LOGGER.warn("[WarForge security] rejected packet from unknown sender: {}", detail);
            return;
        }
        long now = System.currentTimeMillis();
        Buckets state = BUCKETS.computeIfAbsent(player.getUUID(), id -> new Buckets(now));
        synchronized (state) {
            logThrottled(player, state, now, detail);
        }
    }

    private static void logThrottled(ServerPlayer player, Buckets state, long now, String detail) {
        if (now - state.lastLogMs < VIOLATION_LOG_INTERVAL_MS) {
            state.suppressedLogs++;
            return;
        }
        int suppressed = state.suppressedLogs;
        state.suppressedLogs = 0;
        state.lastLogMs = now;
        WarForgeMod.LOGGER.warn("[WarForge security] rejected packet from {} ({}): {}{}",
                player.getName().getString(), player.getUUID(), detail,
                suppressed > 0 ? " (+" + suppressed + " similar suppressed)" : "");
    }

    public static void forget(UUID playerId) {
        if (playerId != null) {
            BUCKETS.remove(playerId);
        }
    }

    public static boolean requireReach(ServerPlayer player, DimBlockPos pos, String action) {
        if (pos == null) {
            violation(player, action + " with no position");
            return false;
        }
        if (!player.level().dimension().equals(pos.dim)) {
            violation(player, action + " in a different dimension (" + Keys.id(pos.dim) + ")");
            return false;
        }
        double dx = (pos.getX() + 0.5D) - player.getX();
        double dy = (pos.getY() + 0.5D) - player.getY();
        double dz = (pos.getZ() + 0.5D) - player.getZ();
        if (dx * dx + dy * dy + dz * dz > MAX_BLOCK_INTERACT_DIST_SQ) {
            violation(player, action + " out of reach at " + pos.toFancyString());
            return false;
        }
        return true;
    }

    public static boolean requireSameDim(ServerPlayer player, ResourceKey<Level> dim, String action) {
        if (dim == null) {
            violation(player, action + " with an unknown dimension");
            return false;
        }
        if (!player.level().dimension().equals(dim)) {
            violation(player, action + " in a different dimension (" + Keys.id(dim) + ")");
            return false;
        }
        return true;
    }

    // Stops crafted claim/unclaim/forceload far from the player.
    public static boolean requireChunkInRange(ServerPlayer player, DimChunkPos chunk, int radius, String action) {
        if (chunk == null) {
            violation(player, action + " with no chunk");
            return false;
        }
        if (!requireSameDim(player, chunk.dim, action)) {
            return false;
        }
        DimChunkPos playerChunk = new DimChunkPos(player.level().dimension(), player.blockPosition());
        int chebyshev = Math.max(Math.abs(chunk.x - playerChunk.x), Math.abs(chunk.z - playerChunk.z));
        if (chebyshev > radius) {
            violation(player, action + " on out-of-range chunk " + chunk + " (" + chebyshev + " > " + radius + ")");
            return false;
        }
        return true;
    }

    public static int claimRange() {
        return WarForgeConfig.CLAIM_MANAGER_RADIUS + 1;
    }

    public static int claimMapRadius() {
        return WarForgeConfig.CLAIM_MANAGER_RADIUS + WarForgeConfig.SIEGE_DECLARE_MAX_RANGE + 1;
    }

    public static boolean requireChunkInClaimRange(ServerPlayer player, DimChunkPos chunk, String action) {
        return requireChunkInRange(player, chunk, WarForgeConfig.CLAIM_MANAGER_RADIUS + 1, action);
    }

    public static ServerLevel resolveLevel(ServerPlayer player, ResourceKey<Level> dim, String action) {
        if (dim == null) {
            violation(player, action + " with an unknown dimension");
            return null;
        }
        ServerLevel level = WarForgeMod.MC_SERVER == null ? null : WarForgeMod.MC_SERVER.getLevel(dim);
        if (level == null) {
            violation(player, action + " in nonexistent dimension " + Keys.id(dim));
        }
        return level;
    }

    public static <T extends BlockEntity> T blockEntityAt(ServerPlayer player, DimBlockPos pos, Class<T> type, String action) {
        ServerLevel level = resolveLevel(player, pos == null ? null : pos.dim, action);
        if (level == null) {
            return null;
        }
        if (!level.isLoaded(pos.toRegularPos())) {
            violation(player, action + " in an unloaded chunk at " + pos.toFancyString());
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos.toRegularPos());
        if (!type.isInstance(be)) {
            violation(player, action + " but there is no " + type.getSimpleName() + " at " + pos.toFancyString());
            return null;
        }
        return type.cast(be);
    }

    /** null => drop; never throws on hostile input. */
    public static ResourceKey<Level> readDim(FriendlyByteBuf data) {
        ResourceLocation id = Ids.tryParse(data.readUtf(MAX_RESOURCE_LOCATION));
        return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
    }

    public static ResourceKey<Level> readDimOrOverworld(FriendlyByteBuf data) {
        ResourceKey<Level> dim = readDim(data);
        return dim == null ? Level.OVERWORLD : dim;
    }

    public static <E extends Enum<E>> E readEnum(FriendlyByteBuf data, E[] values, E fallback) {
        int ordinal = data.readUnsignedByte();
        return ordinal < values.length ? values[ordinal] : fallback;
    }

    public static <E extends Enum<E>> E readEnumInt(FriendlyByteBuf data, E[] values, E fallback) {
        int ordinal = data.readInt();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    /** Clamped to max; callers still tolerate a short buffer. */
    public static int readCount(FriendlyByteBuf data, int max) {
        int count = data.readInt();
        if (count < 0) {
            return 0;
        }
        return Math.min(count, max);
    }

    public static int clampCount(int count, int max) {
        if (count < 0) {
            return 0;
        }
        return Math.min(count, max);
    }
}
