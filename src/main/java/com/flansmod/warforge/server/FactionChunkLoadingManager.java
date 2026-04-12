package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FactionChunkLoadingManager implements ForgeChunkManager.LoadingCallback {
    private final HashMap<UUID, HashMap<Integer, ForgeChunkManager.Ticket>> ticketsByFaction = new HashMap<UUID, HashMap<Integer, ForgeChunkManager.Ticket>>();

    public void initialize() {
        ForgeChunkManager.setForcedChunkLoadingCallback(WarForgeMod.INSTANCE, this);
    }

    public void shutdown() {
        for (Map<Integer, ForgeChunkManager.Ticket> byDim : ticketsByFaction.values()) {
            for (ForgeChunkManager.Ticket ticket : byDim.values()) {
                ForgeChunkManager.releaseTicket(ticket);
            }
        }
        ticketsByFaction.clear();
    }

    public void releaseFaction(UUID factionId) {
        Map<Integer, ForgeChunkManager.Ticket> byDim = ticketsByFaction.remove(factionId);
        if (byDim == null) {
            return;
        }
        for (ForgeChunkManager.Ticket ticket : byDim.values()) {
            ForgeChunkManager.releaseTicket(ticket);
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

        HashMap<Integer, HashSet<DimChunkPos>> chunksByDim = new HashMap<Integer, HashSet<DimChunkPos>>();

        for (DimChunkPos forced : faction.forcedChunks) {
            chunksByDim.computeIfAbsent(forced.dim, dim -> new HashSet<DimChunkPos>()).add(forced);
        }
        for (DimBlockPos collectorPos : faction.islandCollectors) {
            DimChunkPos collectorChunk = collectorPos.toChunkPos();
            chunksByDim.computeIfAbsent(collectorChunk.dim, dim -> new HashSet<DimChunkPos>()).add(collectorChunk);
        }

        HashMap<Integer, ForgeChunkManager.Ticket> ticketsByDim = ticketsByFaction.computeIfAbsent(faction.uuid, id -> new HashMap<Integer, ForgeChunkManager.Ticket>());

        ArrayList<Integer> obsoleteDims = new ArrayList<Integer>();
        for (Integer dim : ticketsByDim.keySet()) {
            if (!chunksByDim.containsKey(dim) || chunksByDim.get(dim).isEmpty()) {
                ForgeChunkManager.releaseTicket(ticketsByDim.get(dim));
                obsoleteDims.add(dim);
            }
        }
        for (Integer dim : obsoleteDims) {
            ticketsByDim.remove(dim);
        }

        for (Map.Entry<Integer, HashSet<DimChunkPos>> entry : chunksByDim.entrySet()) {
            int dim = entry.getKey();
            Set<DimChunkPos> chunks = entry.getValue();
            if (chunks.isEmpty()) {
                continue;
            }

            World world = WarForgeMod.MC_SERVER.getWorld(dim);
            if (world == null) {
                continue;
            }

            ForgeChunkManager.Ticket ticket = ticketsByDim.get(dim);
            if (ticket == null) {
                ticket = ForgeChunkManager.requestTicket(WarForgeMod.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
                if (ticket == null) {
                    WarForgeMod.LOGGER.warn("Failed to allocate chunk-loading ticket for faction {} in dimension {}", faction.name, dim);
                    continue;
                }
                ticketsByDim.put(dim, ticket);
            }

            Set<ChunkPos> currentlyForced = new HashSet<ChunkPos>(ticket.getChunkList());
            for (ChunkPos chunkPos : currentlyForced) {
                ForgeChunkManager.unforceChunk(ticket, chunkPos);
            }

            for (DimChunkPos chunk : chunks) {
                ForgeChunkManager.forceChunk(ticket, new ChunkPos(chunk.x, chunk.z));
            }
        }

        if (ticketsByDim.isEmpty()) {
            ticketsByFaction.remove(faction.uuid);
        }
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        // We rebuild chunk-loading state from faction data on startup.
        for (ForgeChunkManager.Ticket ticket : tickets) {
            ForgeChunkManager.releaseTicket(ticket);
        }
    }
}
