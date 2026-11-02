package com.flansmod.warforge.api;

import com.flansmod.warforge.common.Notifications;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.interfaces.IChunkReinforcer;
import com.flansmod.warforge.api.interfaces.IClaimStrengthModifier;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.VeinSample;
import com.flansmod.warforge.api.vein.VeinView;
import com.flansmod.warforge.api.vein.VeinWeightProvider;
import com.flansmod.warforge.common.ExplosionProtection;
import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.PacketClientNotification;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Chunks;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.FactionStorage;
import com.flansmod.warforge.server.SiegeDifficulty;
import com.flansmod.warforge.server.vein.VeinUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Addon entry point. Server-thread only: every method except {@link #setVeinWeightProvider},
 * {@link #addFobWarpVeto} and {@link #MOD_ID} throws {@link IllegalStateException} elsewhere.
 * Positions are a dimension key plus {@link ChunkPos}/{@link BlockPos}. Nothing loads a chunk.
 * Ownership is not permission: siege state, zones and alliance override it; use {@link #mayPlace}/{@link #mayBreak}.
 */
public final class WarforgeAPI {
    public static final String MOD_ID = Tags.MODID;

    private WarforgeAPI() {}

    /** Same layout as {@code ChunkPos.asLong}: x in the low 32 bits, z in the high 32. */
    public static long pack(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    public static int unpackX(long packed) {
        return (int) packed;
    }

    public static int unpackZ(long packed) {
        return (int) (packed >>> 32);
    }

    private static MinecraftServer server() {
        MinecraftServer server = WarForgeMod.MC_SERVER;
        if (server == null || !server.isSameThread()) {
            throw new IllegalStateException("WarforgeAPI must be called on the server thread");
        }
        return server;
    }

    private static FactionStorage factions() {
        server();
        return WarForgeMod.FACTIONS;
    }

    @Nullable
    private static Faction faction(@Nullable UUID factionId) {
        return FactionIds.isValid(factionId) ? factions().registry.getFaction(factionId) : null;
    }

    /** Faction of a player, or null. */
    @Nullable
    public static UUID factionOf(@Nullable UUID player) {
        if (!FactionIds.isValid(player)) return null;
        Faction faction = factions().registry.getFactionOfPlayer(player);
        return faction == null ? null : faction.getUuid();
    }

    /** Owning player faction; null when unclaimed or a zone. */
    @Nullable
    public static UUID ownerOf(ResourceKey<Level> dim, ChunkPos chunk) {
        UUID owner = factions().claims.getClaim(new DimChunkPos(dim, Chunks.x(chunk), Chunks.z(chunk)));
        return FactionIds.isValid(owner) && !FactionIds.isNeutralZone(owner) ? owner : null;
    }

    public static ZoneKind zoneAt(ResourceKey<Level> dim, ChunkPos chunk) {
        UUID owner = factions().claims.getClaim(new DimChunkPos(dim, Chunks.x(chunk), Chunks.z(chunk)));
        if (!FactionIds.isValid(owner)) return ZoneKind.WILDERNESS;
        if (FactionIds.SAFE_ZONE.equals(owner)) return ZoneKind.SAFE_ZONE;
        if (FactionIds.WAR_ZONE.equals(owner)) return ZoneKind.WAR_ZONE;
        return ZoneKind.FACTION;
    }

    public static SiegeZoneKind siegeZoneAt(ResourceKey<Level> dim, ChunkPos chunk) {
        return switch (factions().sieges.getSiegeZone(new DimChunkPos(dim, Chunks.x(chunk), Chunks.z(chunk))).zone) {
            case SIEGED -> SiegeZoneKind.SIEGED;
            case WAR -> SiegeZoneKind.WAR;
            default -> SiegeZoneKind.NONE;
        };
    }

    /** Owners of the claimed chunks among {@code chunks}; unclaimed chunks are absent. */
    public static Map<ChunkPos, UUID> ownersAlong(ResourceKey<Level> dim, Collection<ChunkPos> chunks) {
        Map<ChunkPos, UUID> out = new LinkedHashMap<>();
        for (ChunkPos chunk : chunks) {
            UUID owner = ownerOf(dim, chunk);
            if (owner != null) out.put(chunk, owner);
        }
        return out;
    }

    public static boolean isMember(UUID factionId, UUID player) {
        Faction faction = faction(factionId);
        return faction != null && faction.isPlayerInFaction(player);
    }

    /** 0 for an unknown faction. */
    public static int memberCount(UUID factionId) {
        Faction faction = faction(factionId);
        return faction == null ? 0 : faction.getMemberCount();
    }

    /** Snapshot of a faction's claimed chunks per dimension as packed chunk longs. */
    public static Map<ResourceKey<Level>, LongSet> claimedChunks(UUID factionId) {
        Faction faction = faction(factionId);
        if (faction == null) return Map.of();
        HashMap<ResourceKey<Level>, LongSet> out = new HashMap<>();
        for (DimBlockPos claim : faction.getClaimPositions()) {
            DimChunkPos chunk = claim.toChunkPos();
            out.computeIfAbsent(chunk.dim, k -> new LongOpenHashSet()).add(pack(chunk.x, chunk.z));
        }
        return Collections.unmodifiableMap(out);
    }

    @Nullable
    public static String factionName(@Nullable UUID factionId) {
        Faction faction = faction(factionId);
        return faction == null ? null : faction.getName();
    }

    /** 0xRRGGBB, or {@code fallback} for an unknown faction. */
    public static int factionColour(@Nullable UUID factionId, int fallback) {
        Faction faction = faction(factionId);
        return faction == null ? fallback : (faction.getColour() & 0xFFFFFF);
    }

    /** Alliance only, a faction with itself included; truce excluded. Checked both ways. */
    public static boolean areAllied(@Nullable UUID a, @Nullable UUID b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        Faction fa = faction(a);
        Faction fb = faction(b);
        return (fa != null && fa.isAllyOf(b)) || (fb != null && fb.isAllyOf(a));
    }

    public static boolean areAlliedOrTruced(@Nullable UUID a, @Nullable UUID b) {
        if (areAllied(a, b)) return true;
        Faction fa = faction(a);
        Faction fb = faction(b);
        return (fa != null && fa.isInTruceWith(b)) || (fb != null && fb.isInTruceWith(a));
    }

    /** Unowned content is nobody's to share: false for null owner. */
    public static boolean maySeeFactionContent(@Nullable UUID viewerPlayerId, @Nullable UUID ownerFactionId) {
        return ownerFactionId != null && areAllied(factionOf(viewerPlayerId), ownerFactionId);
    }

    public static boolean mayPlace(UUID playerId, ResourceKey<Level> dim, ChunkPos chunk, Block block) {
        server();
        return !ProtectionsModule.placeDenied(ProtectionsModule.getProtections(playerId, new DimChunkPos(dim, Chunks.x(chunk), Chunks.z(chunk))), block);
    }

    public static boolean mayBreak(UUID playerId, ResourceKey<Level> dim, ChunkPos chunk, Block block) {
        server();
        return !ProtectionsModule.breakDenied(ProtectionsModule.getProtections(playerId, new DimChunkPos(dim, Chunks.x(chunk), Chunks.z(chunk))), block);
    }

    /** Explosion protection at {@code pos} as if lit by {@code igniterPlayer} (null: no player). */
    public static boolean isExplosionProtected(Level level, BlockPos pos, @Nullable UUID igniterPlayer) {
        server();
        return ExplosionProtection.isProtected(level, igniterPlayer, pos);
    }

    /** Removes protected positions; the igniter is a faction, no player is impersonated. null: no igniter. */
    public static void filterExplosionProtected(Level level, @Nullable UUID igniterFaction, Collection<BlockPos> positions) {
        server();
        ExplosionProtection.filterByFaction(level, igniterFaction, positions);
    }

    /** Toast to every online member. Equal {@code dedupeToken}s replace each other on the client; null: no dedupe. */
    public static void notifyFaction(UUID factionId, @Nullable String dedupeToken, Component message, NotificationStyle style, long durationMs) {
        Faction faction = faction(factionId);
        if (faction == null) return;
        int colour = switch (style) {
            case INFO -> PacketClientNotification.COLOR_INFO;
            case SUCCESS -> PacketClientNotification.COLOR_SUCCESS;
            case WARNING -> PacketClientNotification.COLOR_WARNING;
            case DANGER -> PacketClientNotification.COLOR_DANGER;
        };
        Notifications.notifyFaction(faction, dedupeToken == null ? "" : dedupeToken, message, Component.empty(), colour, (int) durationMs);
    }

    /** Difficulty to siege the chunk now; null when it is not owned by a player faction. */
    @Nullable
    public static SiegeDefence siegeDefenceAt(ResourceKey<Level> dim, ChunkPos chunk) {
        UUID owner = ownerOf(dim, chunk);
        Faction faction = faction(owner);
        return faction == null ? null : SiegeDifficulty.breakdown(server(), faction, new DimChunkPos(dim, Chunks.x(chunk), Chunks.z(chunk)));
    }

    /** Claimed chunks of a faction currently loaded, per dimension. */
    public static Map<ResourceKey<Level>, LongSet> loadedClaimedChunks(UUID factionId) {
        MinecraftServer server = server();
        HashMap<ResourceKey<Level>, LongSet> out = new HashMap<>();
        for (var entry : claimedChunks(factionId).entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) continue;
            for (long packed : entry.getValue()) {
                if (level.getChunkSource().getChunkNow(unpackX(packed), unpackZ(packed)) != null) {
                    out.computeIfAbsent(entry.getKey(), k -> new LongOpenHashSet()).add(packed);
                }
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /** First match in the faction's loaded claimed chunks short-circuits. */
    public static boolean anyLoadedClaimedTile(UUID factionId, Predicate<BlockEntity> predicate) {
        MinecraftServer server = server();
        for (var entry : claimedChunks(factionId).entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) continue;
            for (long packed : entry.getValue()) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(unpackX(packed), unpackZ(packed));
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be != null && !be.isRemoved() && predicate.test(be)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Extra siege difficulty from active reinforcers in the faction's loaded claimed chunks of the dimension.
     * Non-stacking reinforcers contribute only the strongest; stacking ones add. Honours {@link IChunkReinforcer}
     * and the legacy {@link IClaimStrengthModifier}.
     */
    public static int getReinforcementBonus(UUID factionId, ResourceKey<Level> dim, ChunkPos besieged) {
        MinecraftServer server = server();
        LongSet chunks = claimedChunks(factionId).get(dim);
        ServerLevel level = server.getLevel(dim);
        if (chunks == null || level == null) return 0;

        int stackingSum = 0;
        int bestNonStacking = 0;
        for (long packed : chunks) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(unpackX(packed), unpackZ(packed));
            if (chunk == null) continue;

            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be == null || be.isRemoved()) continue;
                int bonus;
                boolean stacks;

                IChunkReinforcer reinforcer = WarForgeCapabilities.chunkReinforcer(level, be);
                if (reinforcer != null) {
                    if (!reinforcer.isReinforcementActive()) continue;
                    ChunkPos rc = Chunks.of(be.getBlockPos());
                    int dist = Math.max(Math.abs(Chunks.x(rc) - Chunks.x(besieged)), Math.abs(Chunks.z(rc) - Chunks.z(besieged)));
                    if (dist > reinforcer.getReinforcementRadius()) continue;
                    bonus = reinforcer.getReinforcementBonus();
                    stacks = reinforcer.stacksWithOthers();
                } else if (be instanceof IClaimStrengthModifier legacy) {
                    if (!legacy.isActive()) continue;
                    ChunkPos rc = Chunks.of(be.getBlockPos());
                    boolean covers = false;
                    for (Vec3i vec : legacy.getEffectArea()) {
                        if (Chunks.x(rc) + vec.getX() == Chunks.x(besieged) && Chunks.z(rc) + vec.getZ() == Chunks.z(besieged)) {
                            covers = true;
                            break;
                        }
                    }
                    if (!covers) continue;
                    bonus = legacy.getClaimContribution();
                    stacks = legacy.canStack();
                } else {
                    continue;
                }

                if (bonus == 0) continue;
                if (stacks) stackingSum += bonus;
                else bestNonStacking = Math.max(bestNonStacking, bonus);
            }
        }
        return stackingSum + bestNonStacking;
    }

    @Nullable
    private static volatile VeinWeightProvider veinWeightProvider;

    /** Survives {@code /f vein reload}. Install before players join: JourneyMap AUTO rolls veins around them. Any thread. */
    public static void setVeinWeightProvider(@Nullable VeinWeightProvider provider) {
        veinWeightProvider = provider;
    }

    @Nullable
    public static VeinWeightProvider veinWeightProvider() {
        return veinWeightProvider;
    }

    /** All configured veins; empty before veins load. */
    public static List<VeinView> veins() {
        VeinUtils handler = WarForgeMod.VEIN_HANDLER;
        server();
        if (handler == null) return List.of();
        ArrayList<VeinView> out = new ArrayList<>(handler.ID_TO_VEINS.size());
        for (Vein vein : handler.ID_TO_VEINS.values()) {
            out.add(vein.view());
        }
        return List.copyOf(out);
    }

    /** By translation key or numeric id; null when unknown. */
    @Nullable
    public static VeinView findVein(String key) {
        server();
        VeinUtils handler = WarForgeMod.VEIN_HANDLER;
        Vein vein = handler == null ? null : handler.findVein(key);
        return vein == null ? null : vein.view();
    }

    /** The chunk's vein (rolled and stored on first query); null for no vein or veins not loaded. */
    @Nullable
    public static VeinSample veinAt(ServerLevel level, int chunkX, int chunkZ) {
        server();
        VeinUtils handler = WarForgeMod.VEIN_HANDLER;
        if (handler == null || !handler.hasFinishedInit) return null;
        var info = handler.getVein(level.dimension(), chunkX, chunkZ, level.getSeed());
        if (info == null || info.getLeft() == null) return null;
        Quality quality = info.getRight();
        return new VeinSample(info.getLeft().view(), quality);
    }

    private static final List<FobWarpVeto> FOB_WARP_VETOES = new CopyOnWriteArrayList<>();

    /** Any thread. */
    public static void addFobWarpVeto(FobWarpVeto veto) {
        FOB_WARP_VETOES.add(veto);
    }

    /** First veto reason, or null. For the FOB manager. */
    @Nullable
    public static Component fobWarpVeto(ServerPlayer player, FobWarpVeto.Target fob) {
        for (FobWarpVeto veto : FOB_WARP_VETOES) {
            Component reason = veto.check(player, fob);
            if (reason != null) return reason;
        }
        return null;
    }
}
