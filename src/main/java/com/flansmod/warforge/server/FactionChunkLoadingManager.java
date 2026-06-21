package com.flansmod.warforge.server;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FactionChunkLoadingManager {
    private final HashMap<UUID, HashMap<ResourceKey<Level>, HashSet<DimChunkPos>>> forcedByFaction = new HashMap<UUID, HashMap<ResourceKey<Level>, HashSet<DimChunkPos>>>();

    public void initialize() {
        ForgeChunkManager.setForcedChunkLoadingCallback(Tags.MODID, this::validateTickets);
    }

    // Runs as forced chunks are reinstated on level load. Drop any UUID-owned tickets whose faction
    // no longer exists (or is a neutral zone); tickets for still-existing factions are left in place.
    private void validateTickets(ServerLevel level, ForgeChunkManager.TicketHelper ticketHelper) {
        for (UUID factionId : ticketHelper.getEntityTickets().keySet()) {
            Faction faction = WarForgeMod.FACTIONS.getFaction(factionId);
            if (faction == null || !FactionStorage.isValidFaction(faction) || FactionStorage.IsNeutralZone(factionId)) {
                ticketHelper.removeAllTickets(factionId);
            }
        }
    }

    public void shutdown() {
        for (UUID factionId : new HashSet<UUID>(forcedByFaction.keySet())) {
            releaseFaction(factionId);
        }
        forcedByFaction.clear();
    }

    public void releaseFaction(UUID factionId) {
        Map<ResourceKey<Level>, HashSet<DimChunkPos>> byDim = forcedByFaction.remove(factionId);
        if (byDim == null) {
            return;
        }
        for (Map.Entry<ResourceKey<Level>, HashSet<DimChunkPos>> entry : byDim.entrySet()) {
            ServerLevel level = WarForgeMod.MC_SERVER.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            for (DimChunkPos chunk : entry.getValue()) {
                ForgeChunkManager.forceChunk(level, Tags.MODID, factionId, chunk.x, chunk.z, false, false);
            }
        }
    }

    public void refreshAllFactions(Collection<Faction> factions) {
        for (Faction faction : factions) {
            if (faction != null && FactionStorage.isValidFaction(faction) && !FactionStorage.IsNeutralZone(faction.uuid)) {
                refreshFactionChunks(faction);
            }
        }
    }

    public void refreshFactionChunks(Faction faction) {
        if (faction == null || !FactionStorage.isValidFaction(faction)) {
            return;
        }

        HashMap<ResourceKey<Level>, HashSet<DimChunkPos>> desiredByDim = new HashMap<ResourceKey<Level>, HashSet<DimChunkPos>>();

        for (DimChunkPos forced : faction.forcedChunks) {
            desiredByDim.computeIfAbsent(forced.dim, dim -> new HashSet<DimChunkPos>()).add(forced);
        }
        for (DimBlockPos collectorPos : faction.islandCollectors) {
            DimChunkPos collectorChunk = collectorPos.toChunkPos();
            desiredByDim.computeIfAbsent(collectorChunk.dim, dim -> new HashSet<DimChunkPos>()).add(collectorChunk);
        }

        HashMap<ResourceKey<Level>, HashSet<DimChunkPos>> currentByDim = forcedByFaction.computeIfAbsent(faction.uuid, id -> new HashMap<ResourceKey<Level>, HashSet<DimChunkPos>>());

        for (Map.Entry<ResourceKey<Level>, HashSet<DimChunkPos>> entry : currentByDim.entrySet()) {
            ResourceKey<Level> dim = entry.getKey();
            HashSet<DimChunkPos> desired = desiredByDim.get(dim);
            ServerLevel level = WarForgeMod.MC_SERVER.getLevel(dim);
            if (level == null) {
                continue;
            }
            for (DimChunkPos chunk : new HashSet<DimChunkPos>(entry.getValue())) {
                if (desired == null || !desired.contains(chunk)) {
                    ForgeChunkManager.forceChunk(level, Tags.MODID, faction.uuid, chunk.x, chunk.z, false, false);
                    entry.getValue().remove(chunk);
                }
            }
        }

        for (Map.Entry<ResourceKey<Level>, HashSet<DimChunkPos>> entry : desiredByDim.entrySet()) {
            ResourceKey<Level> dim = entry.getKey();
            Set<DimChunkPos> chunks = entry.getValue();
            if (chunks.isEmpty()) {
                continue;
            }

            ServerLevel level = WarForgeMod.MC_SERVER.getLevel(dim);
            if (level == null) {
                continue;
            }

            HashSet<DimChunkPos> current = currentByDim.computeIfAbsent(dim, id -> new HashSet<DimChunkPos>());
            for (DimChunkPos chunk : chunks) {
                if (current.add(chunk)) {
                    ForgeChunkManager.forceChunk(level, Tags.MODID, faction.uuid, chunk.x, chunk.z, true, false);
                }
            }
        }

        currentByDim.values().removeIf(Set::isEmpty);
        if (currentByDim.isEmpty()) {
            forcedByFaction.remove(faction.uuid);
        }
    }
}
