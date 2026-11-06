package com.flansmod.warforge.client;

import com.flansmod.warforge.client.render.FlashbackReplay;
import com.flansmod.warforge.common.ProtectionProfile;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.network.PacketClaimCacheState;
import com.flansmod.warforge.common.network.PacketClaimMapDelta;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sync replaces its window only: claims seen earlier stay known outside it. Main thread writes; Xaero map threads read
 * per-dimension colour snapshots published through a concurrent map.
 */
public final class ClientBorderCache {
    public static final int NO_CLAIM = Integer.MIN_VALUE;
    private static final int REGION_SHIFT = 5;

    public interface Listener {
        void onClaimSet(ResourceKey<Level> dim, int x, int z, int colour);

        void onClaimRemoved(ResourceKey<Level> dim, int x, int z);

        void onCleared();
    }

    private static final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2ObjectOpenHashMap<ClaimChunkInfo>> BY_DIM = new Object2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<ClaimChunkInfo> EMPTY = new Long2ObjectOpenHashMap<>();
    private static final Map<ResourceKey<Level>, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private static boolean changed;

    private ClientBorderCache() {
    }

    public static long key(int x, int z) {
        return (long) x & 0xFFFFFFFFL | (long) z << 32;
    }

    public static void replaceWindow(ResourceKey<Level> dim, int centerX, int centerZ, int radius, Collection<ClaimChunkInfo> chunks) {
        Long2ObjectOpenHashMap<ClaimChunkInfo> map = BY_DIM.computeIfAbsent(dim, d -> new Long2ObjectOpenHashMap<>());
        Long2ObjectOpenHashMap<ClaimChunkInfo> incoming = new Long2ObjectOpenHashMap<>(chunks.size());
        for (ClaimChunkInfo info : chunks) {
            incoming.put(key(info.x, info.z), info);
        }

        boolean colourChanged = false;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                long key = key(x, z);
                ClaimChunkInfo old = map.get(key);
                ClaimChunkInfo fresh = incoming.get(key);
                if (old == null && fresh == null) {
                    continue;
                }
                if (fresh == null) {
                    map.remove(key);
                } else {
                    map.put(key, fresh);
                }
                int oldColour = colourOf(old);
                int newColour = colourOf(fresh);
                if (oldColour != newColour) {
                    colourChanged = true;
                    notifyChange(dim, x, z, newColour);
                }
            }
        }
        if (colourChanged) {
            SNAPSHOTS.put(dim, new Snapshot(map));
        }
        changed = true;
    }

    public static void applyWorldDelta(PacketClaimMapDelta delta) {
        ObjectOpenHashSet<ResourceKey<Level>> touched = new ObjectOpenHashSet<>();
        for (PacketClaimMapDelta.Set set : delta.sets) {
            Long2ObjectOpenHashMap<ClaimChunkInfo> map = BY_DIM.computeIfAbsent(set.dim(), d -> new Long2ObjectOpenHashMap<>());
            long key = key(set.x(), set.z());
            ClaimChunkInfo old = map.get(key);
            if (old != null && old.factionId.equals(set.owner()) && old.colour == set.colour()) {
                continue;
            }
            ClaimChunkInfo info = new ClaimChunkInfo();
            info.x = set.x();
            info.z = set.z();
            info.factionId = set.owner();
            info.colour = set.colour();
            info.protection = ProtectionProfile.UNKNOWN;
            // Live: next window sync supplies the outline. Replay: recorded windows cover only the recorded player's surroundings.
            if (FlashbackReplay.active()) {
                claimOutlineIfNone(info);
            }
            map.put(key, info);
            touched.add(set.dim());
            notifyChange(set.dim(), set.x(), set.z(), set.colour());
        }
        for (PacketClaimMapDelta.Remove remove : delta.removes) {
            Long2ObjectOpenHashMap<ClaimChunkInfo> map = BY_DIM.get(remove.dim());
            if (map != null && map.remove(key(remove.x(), remove.z())) != null) {
                touched.add(remove.dim());
                notifyChange(remove.dim(), remove.x(), remove.z(), NO_CLAIM);
            }
        }
        for (ResourceKey<Level> dim : touched) {
            SNAPSHOTS.put(dim, new Snapshot(BY_DIM.get(dim)));
        }
        changed = !touched.isEmpty() || changed;
    }

    public static void applyState(PacketClaimCacheState state) {
        if (state.first) {
            clear();
        }
        Long2ObjectOpenHashMap<ClaimChunkInfo> map = BY_DIM.computeIfAbsent(state.dim, d -> new Long2ObjectOpenHashMap<>());
        for (PacketClaimCacheState.Entry entry : state.entries) {
            ClaimChunkInfo info = new ClaimChunkInfo();
            info.x = entry.x();
            info.z = entry.z();
            info.factionId = entry.owner();
            info.colour = entry.colour();
            info.outlineFactionId = entry.outlineOwner();
            info.outlineColour = entry.outlineColour();
            info.outlineStyle = entry.outlineStyle();
            info.protection = ProtectionProfile.UNKNOWN;
            claimOutlineIfNone(info);
            map.put(key(info.x, info.z), info);
            int colour = colourOf(info);
            if (colour != NO_CLAIM) {
                notifyChange(state.dim, info.x, info.z, colour);
            }
        }
        SNAPSHOTS.put(state.dim, new Snapshot(map));
        changed = true;
    }

    /** At least one packet: an empty first one still replaces a later state on a backward seek. */
    public static List<PacketClaimCacheState> captureState() {
        List<PacketClaimCacheState> out = new ArrayList<>();
        PacketClaimCacheState batch = new PacketClaimCacheState();
        batch.first = true;
        for (Map.Entry<ResourceKey<Level>, Long2ObjectOpenHashMap<ClaimChunkInfo>> dim : BY_DIM.entrySet()) {
            if (batch.dim != dim.getKey() && !batch.entries.isEmpty()) {
                out.add(batch);
                batch = new PacketClaimCacheState();
            }
            batch.dim = dim.getKey();
            for (ClaimChunkInfo info : dim.getValue().values()) {
                if (batch.entries.size() == PacketClaimCacheState.MAX_ENTRIES) {
                    out.add(batch);
                    batch = new PacketClaimCacheState();
                    batch.dim = dim.getKey();
                }
                batch.entries.add(new PacketClaimCacheState.Entry(info.x, info.z, info.factionId, info.colour,
                        info.outlineFactionId, info.outlineColour, info.outlineStyle));
            }
        }
        out.add(batch);
        return out;
    }

    private static void claimOutlineIfNone(ClaimChunkInfo info) {
        if (!info.hasVisibleOutline() && FactionIds.isValid(info.factionId)) {
            info.outlineFactionId = info.factionId;
            info.outlineColour = info.colour;
            info.outlineStyle = ClaimChunkInfo.OUTLINE_CLAIM;
        }
    }

    public static void clear() {
        BY_DIM.clear();
        SNAPSHOTS.clear();
        changed = true;
        for (Listener listener : LISTENERS) {
            listener.onCleared();
        }
    }

    public static boolean consumeChanged() {
        boolean wasChanged = changed;
        changed = false;
        return wasChanged;
    }

    public static Long2ObjectOpenHashMap<ClaimChunkInfo> chunks(ResourceKey<Level> dim) {
        Long2ObjectOpenHashMap<ClaimChunkInfo> map = BY_DIM.get(dim);
        return map == null ? EMPTY : map;
    }

    public static ClaimChunkInfo get(ResourceKey<Level> dim, int x, int z) {
        Long2ObjectOpenHashMap<ClaimChunkInfo> map = BY_DIM.get(dim);
        return map == null ? null : map.get(key(x, z));
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
        for (Map.Entry<ResourceKey<Level>, Snapshot> entry : SNAPSHOTS.entrySet()) {
            var it = entry.getValue().colours.long2IntEntrySet().fastIterator();
            while (it.hasNext()) {
                var claim = it.next();
                listener.onClaimSet(entry.getKey(), (int) claim.getLongKey(), (int) (claim.getLongKey() >> 32), claim.getIntValue());
            }
        }
    }

    public static int claimColour(ResourceKey<Level> dim, int x, int z) {
        Snapshot snapshot = SNAPSHOTS.get(dim);
        return snapshot == null ? NO_CLAIM : snapshot.colours.get(key(x, z));
    }

    /** Any thread. */
    public static boolean regionHasClaims(ResourceKey<Level> dim, int regionX, int regionZ) {
        Snapshot snapshot = SNAPSHOTS.get(dim);
        return snapshot != null && snapshot.regionClaims.get(key(regionX, regionZ)) > 0;
    }

    /**
     * Changes whenever a claim in the region or one of its eight neighbours changes (a neighbour decides where this
     * region's borders are). Any thread.
     */
    public static int regionRevision(ResourceKey<Level> dim, int regionX, int regionZ) {
        Snapshot snapshot = SNAPSHOTS.get(dim);
        if (snapshot == null) {
            return 0;
        }
        int revision = 1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                revision = 31 * revision + snapshot.regionHash.get(key(regionX + dx, regionZ + dz));
            }
        }
        return revision;
    }

    private static int colourOf(ClaimChunkInfo info) {
        return info == null || !FactionIds.isValid(info.factionId) ? NO_CLAIM : info.colour;
    }

    private static void notifyChange(ResourceKey<Level> dim, int x, int z, int colour) {
        for (Listener listener : LISTENERS) {
            if (colour == NO_CLAIM) {
                listener.onClaimRemoved(dim, x, z);
            } else {
                listener.onClaimSet(dim, x, z, colour);
            }
        }
    }

    private static final class Snapshot {
        final Long2IntOpenHashMap colours = new Long2IntOpenHashMap();
        final Long2IntOpenHashMap regionClaims = new Long2IntOpenHashMap();
        final Long2IntOpenHashMap regionHash = new Long2IntOpenHashMap();

        Snapshot(Long2ObjectOpenHashMap<ClaimChunkInfo> chunks) {
            colours.defaultReturnValue(NO_CLAIM);
            var it = chunks.long2ObjectEntrySet().fastIterator();
            while (it.hasNext()) {
                var entry = it.next();
                int colour = colourOf(entry.getValue());
                if (colour == NO_CLAIM) {
                    continue;
                }
                colours.put(entry.getLongKey(), colour);
                long region = key(entry.getValue().x >> REGION_SHIFT, entry.getValue().z >> REGION_SHIFT);
                regionClaims.addTo(region, 1);
                regionHash.addTo(region, Long.hashCode(entry.getLongKey() * 0x9E3779B97F4A7C15L + colour));
            }
        }
    }
}
