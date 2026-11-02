package com.flansmod.warforge.compat.dynmap;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.ChunkOutline;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Keys;
import com.flansmod.warforge.compat.PlayerRef;
import com.flansmod.warforge.compat.Profiles;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.FactionStorage;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server thread only: marker calls are made from {@link #tick}, the listener only hands the API over.
 * Claims: one pass over the owner index per ClaimRegistry revision; geometry rebuilt only for owners whose
 * (count, chunk hash) changed. Name, colour, members, leader and style changes restyle existing markers.
 */
final class DynmapLayer {
    private static final String SET_ID = "warforge.factions";
    private static final int SAFE_ZONE_COLOUR = 0x55FF55;
    private static final int WAR_ZONE_COLOUR = 0xFF5555;
    private static final int STROKE_WEIGHT = 2;

    private static FactionStorage hub;
    private static DynmapCommonAPIListener listener;
    private static volatile DynmapCommonAPI pendingApi;
    private static volatile boolean apiLost;
    private static MarkerSet set;
    private static final Map<UUID, Owner> owners = new HashMap<>();
    private static final Map<ResourceKey<Level>, String> worldNames = new HashMap<>();
    private static int lastRevision = -1;
    private static int ticks;

    private DynmapLayer() {
    }

    private static final class Owner {
        final List<AreaMarker> markers = new ArrayList<>();
        int count;
        long hash;
        String name;
        int colour;
        int members;
        UUID leader;
        double fill;
        double stroke;
    }

    static void start(FactionStorage storage) {
        hub = storage;
        listener = new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI api) {
                pendingApi = api;
            }

            @Override
            public void apiDisabled(DynmapCommonAPI api) {
                apiLost = true;
            }
        };
        DynmapCommonAPIListener.register(listener);
    }

    static void stop() {
        DynmapCommonAPIListener.unregister(listener);
        reset();
        pendingApi = null;
        listener = null;
        hub = null;
    }

    private static void reset() {
        set = null;
        owners.clear();
        worldNames.clear();
        lastRevision = -1;
        ticks = 0;
    }

    static void tick() {
        if (apiLost) {
            apiLost = false;
            reset();
        }
        DynmapCommonAPI api = pendingApi;
        if (api != null) {
            pendingApi = null;
            reset();
            createSet(api.getMarkerAPI());
        }
        if (set == null || ++ticks < WarForgeConfig.DYNMAP_UPDATE_SECONDS * 20) {
            return;
        }
        ticks = 0;
        refresh();
    }

    private static void createSet(MarkerAPI markers) {
        MarkerSet stale = markers.getMarkerSet(SET_ID);
        if (stale != null) {
            stale.deleteMarkerSet();
        }
        set = markers.createMarkerSet(SET_ID, WarForgeConfig.DYNMAP_LAYER_LABEL, null, false);
        set.setHideByDefault(WarForgeConfig.DYNMAP_HIDE_BY_DEFAULT);
        set.setLayerPriority(WarForgeConfig.DYNMAP_LAYER_PRIORITY);
        WarForgeMod.LOGGER.info("Dynmap layer '{}' registered", WarForgeConfig.DYNMAP_LAYER_LABEL);
    }

    private static void refresh() {
        int revision = hub.claims.getRevision();
        HashSet<UUID> dirty = null;
        if (revision != lastRevision) {
            lastRevision = revision;
            dirty = scanClaims();
        }
        Iterator<Map.Entry<UUID, Owner>> it = owners.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Owner> entry = it.next();
            Owner owner = entry.getValue();
            if (owner.count == 0 || !isDrawable(entry.getKey())) {
                clear(owner);
                it.remove();
            } else if (dirty == null || !dirty.contains(entry.getKey())) {
                restyleIfChanged(entry.getKey(), owner);
            }
        }
        if (dirty != null && !dirty.isEmpty()) {
            rebuild(dirty);
        }
    }

    private static boolean isDrawable(UUID id) {
        return FactionIds.isNeutralZone(id) || hub.registry.getFaction(id) != null;
    }

    /** Updates counts and hashes of every owner; returns owners whose chunk set changed. */
    private static HashSet<UUID> scanClaims() {
        HashMap<UUID, long[]> sums = new HashMap<>();
        for (Map.Entry<DimChunkPos, UUID> claim : hub.claims.getClaims().entrySet()) {
            long[] sum = sums.computeIfAbsent(claim.getValue(), k -> new long[2]);
            sum[0]++;
            DimChunkPos chunk = claim.getKey();
            sum[1] += HashCommon.mix(ChunkOutline.key(chunk.x, chunk.z) ^ ((long) chunk.dim.hashCode() << 17));
        }
        HashSet<UUID> dirty = new HashSet<>();
        for (Map.Entry<UUID, long[]> entry : sums.entrySet()) {
            Owner owner = owners.get(entry.getKey());
            long[] sum = entry.getValue();
            if (owner == null) {
                if (!isDrawable(entry.getKey())) {
                    continue;
                }
                owner = new Owner();
                owners.put(entry.getKey(), owner);
            }
            if (owner.count != sum[0] || owner.hash != sum[1]) {
                owner.count = (int) sum[0];
                owner.hash = sum[1];
                dirty.add(entry.getKey());
            }
        }
        for (Map.Entry<UUID, Owner> entry : owners.entrySet()) {
            if (!sums.containsKey(entry.getKey())) {
                entry.getValue().count = 0;
            }
        }
        return dirty;
    }

    private static void rebuild(HashSet<UUID> dirty) {
        HashMap<UUID, HashMap<ResourceKey<Level>, LongOpenHashSet>> chunks = new HashMap<>();
        for (Map.Entry<DimChunkPos, UUID> claim : hub.claims.getClaims().entrySet()) {
            if (dirty.contains(claim.getValue())) {
                chunks.computeIfAbsent(claim.getValue(), k -> new HashMap<>())
                        .computeIfAbsent(claim.getKey().dim, k -> new LongOpenHashSet())
                        .add(ChunkOutline.key(claim.getKey().x, claim.getKey().z));
            }
        }
        for (Map.Entry<UUID, HashMap<ResourceKey<Level>, LongOpenHashSet>> entry : chunks.entrySet()) {
            Owner owner = owners.get(entry.getKey());
            if (owner == null) {
                continue;
            }
            clear(owner);
            captureAppearance(entry.getKey(), owner);
            String description = describe(entry.getKey(), owner);
            int id = 0;
            for (Map.Entry<ResourceKey<Level>, LongOpenHashSet> dim : entry.getValue().entrySet()) {
                String world = worldName(dim.getKey());
                for (int[] polygon : ChunkOutline.outlines(dim.getValue())) {
                    double[] xs = new double[polygon.length / 2];
                    double[] zs = new double[xs.length];
                    for (int i = 0; i < xs.length; i++) {
                        xs[i] = polygon[2 * i] * 16.0;
                        zs[i] = polygon[2 * i + 1] * 16.0;
                    }
                    AreaMarker area = set.createAreaMarker("wf_" + entry.getKey() + "_" + id++, owner.name, false, world, xs, zs, false);
                    if (area != null) {
                        area.setDescription(description);
                        style(area, owner);
                        owner.markers.add(area);
                    }
                }
            }
        }
    }

    private static void restyleIfChanged(UUID id, Owner owner) {
        boolean zone = FactionIds.isNeutralZone(id);
        Faction faction = zone ? null : hub.registry.getFaction(id);
        int colour = zone ? zoneColour(id) : faction.getColour() & 0xFFFFFF;
        if (colour == owner.colour
                && owner.fill == WarForgeConfig.DYNMAP_FILL_OPACITY
                && owner.stroke == WarForgeConfig.DYNMAP_STROKE_OPACITY
                && (zone || (owner.members == faction.getMemberCount()
                && owner.name.equals(faction.getName())
                && owner.leader.equals(faction.getLeaderId())))) {
            return;
        }
        captureAppearance(id, owner);
        String description = describe(id, owner);
        for (AreaMarker area : owner.markers) {
            area.setLabel(owner.name);
            area.setDescription(description);
            style(area, owner);
        }
    }

    private static void captureAppearance(UUID id, Owner owner) {
        owner.fill = WarForgeConfig.DYNMAP_FILL_OPACITY;
        owner.stroke = WarForgeConfig.DYNMAP_STROKE_OPACITY;
        if (FactionIds.isNeutralZone(id)) {
            owner.colour = zoneColour(id);
            owner.name = FactionIds.SAFE_ZONE.equals(id) ? WarForgeConfig.DYNMAP_SAFE_ZONE_LABEL : WarForgeConfig.DYNMAP_WAR_ZONE_LABEL;
            owner.members = 0;
            owner.leader = FactionIds.NONE;
            return;
        }
        Faction faction = hub.registry.getFaction(id);
        owner.colour = faction.getColour() & 0xFFFFFF;
        owner.name = faction.getName();
        owner.members = faction.getMemberCount();
        owner.leader = faction.getLeaderId();
    }

    private static int zoneColour(UUID id) {
        return FactionIds.SAFE_ZONE.equals(id) ? SAFE_ZONE_COLOUR : WAR_ZONE_COLOUR;
    }

    private static void style(AreaMarker area, Owner owner) {
        area.setLineStyle(STROKE_WEIGHT, owner.stroke, owner.colour);
        area.setFillStyle(owner.fill, owner.colour);
    }

    private static String describe(UUID id, Owner owner) {
        StringBuilder html = new StringBuilder(160);
        html.append("<div class=\"infowindow\"><span style=\"font-weight:bold;\">").append(escape(owner.name)).append("</span>");
        if (!FactionIds.isNeutralZone(id)) {
            Optional<PlayerRef> leader = Profiles.get(WarForgeMod.MC_SERVER, owner.leader);
            html.append("<br/>Leader: ").append(leader.isPresent() ? escape(leader.get().getName()) : "?");
            html.append("<br/>Members: ").append(owner.members);
        }
        return html.append("<br/>Chunks: ").append(owner.count).append("</div>").toString();
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static void clear(Owner owner) {
        for (AreaMarker area : owner.markers) {
            area.deleteMarker();
        }
        owner.markers.clear();
    }

    /** Dynmap Forge world naming: level name, DIM-1, DIM1, else namespace_path. */
    private static String worldName(ResourceKey<Level> dim) {
        return worldNames.computeIfAbsent(dim, key -> {
            if (key == Level.OVERWORLD) {
                return WarForgeMod.MC_SERVER.getWorldData().getLevelName();
            }
            if (key == Level.NETHER) {
                return "DIM-1";
            }
            if (key == Level.END) {
                return "DIM1";
            }
            var id = Keys.id(key);
            return id.getNamespace() + "_" + id.getPath();
        });
    }
}
