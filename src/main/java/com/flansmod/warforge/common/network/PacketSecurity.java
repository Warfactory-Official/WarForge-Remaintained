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

/**
 * Server-side gatekeeping for everything that arrives on the WarForge channel.
 *
 * <p>Nothing a client sends is trusted: positions, dimensions, faction UUIDs, enum ordinals and
 * collection lengths are all attacker-controlled. The GUIs enforce plenty of rules (you can only
 * click chunks near you, only the citadel placer gets the "create faction" button, ...), but a
 * crafted packet skips the GUI entirely, so every one of those rules has to exist here too.
 *
 * <p>Three layers live in this class:
 * <ul>
 *   <li><b>Decode bounds</b> - {@code readDim}/{@code readEnum}/{@code readCount} never allocate or
 *       throw on hostile input, so a malformed packet can't OOM or kill the netty pipeline.</li>
 *   <li><b>Authority</b> - {@code requireReach}/{@code requireSameDim}/{@code requireChunkInRange}
 *       bind an action to where the player actually is.</li>
 *   <li><b>Rate limiting</b> - a per-player token bucket per {@link Bucket}, applied centrally in
 *       {@link PacketHandler} so no packet can be added without a cost.</li>
 * </ul>
 */
public final class PacketSecurity {
    private PacketSecurity() {
    }

    // ---------------------------------------------------------------- string caps

    /** Namespaced ids are 256 chars at the very most; anything longer is junk. */
    public static final int MAX_RESOURCE_LOCATION = 256;
    /**
     * Faction / player / FOB names. Matches the ceiling of WarForgeConfig "Max Faction Name Length"
     * so a server that raises that setting still round-trips; the real length rule is enforced
     * server-side by validateFactionName.
     */
    public static final int MAX_NAME = 128;
    /** Flag ids are resource-path shaped. */
    public static final int MAX_FLAG_ID = 128;

    /**
     * Blocks a player may act on through a packet. Vanilla reach is ~4.5 (6 in creative); we allow a
     * bit more so latency or a slightly stale GUI never rejects a legitimate click.
     */
    public static final double MAX_BLOCK_INTERACT_DIST = 12.0D;
    private static final double MAX_BLOCK_INTERACT_DIST_SQ = MAX_BLOCK_INTERACT_DIST * MAX_BLOCK_INTERACT_DIST;

    // ---------------------------------------------------------------- rate limiting

    /**
     * Cost classes. Capacity is the burst a client may spend at once; refill is sustained rate per
     * second. Sized so the real GUIs (which fire a request per keystroke / per map pan) never hit the
     * limit, while a scripted flood is throttled to something the server can absorb.
     */
    public enum Bucket {
        /**
         * Cheap lookups against in-memory state: faction info, leaderboard, nameplates, veins.
         * The burst is large because joining a busy server fires one nameplate request per visible
         * player in a single frame.
         */
        QUERY(150, 50),
        /**
         * Claim-map windows. Each request walks a (2r+1)^2 chunk square - up to 9409 lookups at the
         * border-sync radius - so this is the one read path worth keeping tight. The client resyncs
         * on chunk change plus a slow safety interval, nowhere near this rate.
         */
        MAP(12, 4),
        /** Terrain colour sampling: reads 256 heightmap+blockstate samples per chunk in the window. */
        TERRAIN(8, 3),
        /** State mutations: claiming, sieging, alliances, faction lifecycle. */
        ACTION(20, 8),
        /** Packets that carry no server-side work at all. */
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

    /** One violation log per player per this many ms, so hostile traffic can't also flood the log. */
    private static final long VIOLATION_LOG_INTERVAL_MS = 5_000L;

    /**
     * Spends one token from {@code bucket} for {@code player}. Returns false when the player is over
     * budget, in which case the caller must drop the packet.
     */
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

    /**
     * Records something a well-behaved client cannot do. The packet is always dropped by the caller;
     * this only decides whether it gets written to the log.
     */
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

    /** Drops a disconnecting player's buckets so the map doesn't grow without bound. */
    public static void forget(UUID playerId) {
        if (playerId != null) {
            BUCKETS.remove(playerId);
        }
    }

    // ---------------------------------------------------------------- authority checks

    /** True when {@code pos} is in the player's dimension and close enough to touch. */
    public static boolean requireReach(ServerPlayer player, DimBlockPos pos, String action) {
        if (pos == null) {
            violation(player, action + " with no position");
            return false;
        }
        if (!player.level().dimension().equals(pos.dim)) {
            violation(player, action + " in a different dimension (" + pos.dim.location() + ")");
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
            violation(player, action + " in a different dimension (" + dim.location() + ")");
            return false;
        }
        return true;
    }

    /**
     * True when {@code chunk} lies inside the square window the claim-manager UI can actually show
     * around the player. This is what stops a crafted packet from claiming, unclaiming or force
     * loading chunks on the far side of the world.
     */
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

    /** Claim-manager window plus a chunk of slack for players walking while the GUI is open. */
    public static boolean requireChunkInClaimRange(ServerPlayer player, DimChunkPos chunk, String action) {
        return requireChunkInRange(player, chunk, WarForgeConfig.CLAIM_MANAGER_RADIUS + 1, action);
    }

    /**
     * Resolves a client-supplied dimension to a real level. Positions decoded from packets are
     * routinely fed to {@code MC_SERVER.getLevel(...)}; without this a bogus dimension id is a null
     * dereference.
     */
    public static ServerLevel resolveLevel(ServerPlayer player, ResourceKey<Level> dim, String action) {
        if (dim == null) {
            violation(player, action + " with an unknown dimension");
            return null;
        }
        ServerLevel level = WarForgeMod.MC_SERVER == null ? null : WarForgeMod.MC_SERVER.getLevel(dim);
        if (level == null) {
            violation(player, action + " in nonexistent dimension " + dim.location());
        }
        return level;
    }

    /**
     * Fetches a block entity of the expected type at a client-supplied position, without loading
     * chunks and without a ClassCastException when the client points at something else.
     */
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

    // ---------------------------------------------------------------- bounded decoding

    /**
     * Reads a dimension key. Returns null (rather than throwing out of the netty pipeline) when the
     * client sends an over-long or syntactically invalid id; handlers treat null as "drop".
     */
    public static ResourceKey<Level> readDim(FriendlyByteBuf data) {
        ResourceLocation id = ResourceLocation.tryParse(data.readUtf(MAX_RESOURCE_LOCATION));
        return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
    }

    /** Reads a dimension key, falling back to the overworld so downstream code sees a non-null key. */
    public static ResourceKey<Level> readDimOrOverworld(FriendlyByteBuf data) {
        ResourceKey<Level> dim = readDim(data);
        return dim == null ? Level.OVERWORLD : dim;
    }

    /** Reads an enum by unsigned-byte ordinal, substituting {@code fallback} for out-of-range values. */
    public static <E extends Enum<E>> E readEnum(FriendlyByteBuf data, E[] values, E fallback) {
        int ordinal = data.readUnsignedByte();
        return ordinal < values.length ? values[ordinal] : fallback;
    }

    /** Reads an int-encoded enum ordinal, substituting {@code fallback} for out-of-range values. */
    public static <E extends Enum<E>> E readEnumInt(FriendlyByteBuf data, E[] values, E fallback) {
        int ordinal = data.readInt();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    /**
     * Reads a collection length and clamps it to {@code max}. Callers must still tolerate a short
     * buffer; this only stops a small packet from asking for a huge allocation up front.
     */
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
