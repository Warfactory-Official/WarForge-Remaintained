package com.flansmod.warforge.server;

import com.flansmod.warforge.Tags;
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
    private final HashMap<UUID, HashMap<Integer, List<ForgeChunkManager.Ticket>>> ticketsByFaction = new HashMap<UUID, HashMap<Integer, List<ForgeChunkManager.Ticket>>>();

    public void initialize() {
        ForgeChunkManager.setForcedChunkLoadingCallback(WarForgeMod.INSTANCE, this);
    }

    public void shutdown() {
        for (Map<Integer, List<ForgeChunkManager.Ticket>> byDim : ticketsByFaction.values()) {
            for (List<ForgeChunkManager.Ticket> tickets : byDim.values()) {
                for (ForgeChunkManager.Ticket ticket : tickets) {
                    ForgeChunkManager.releaseTicket(ticket);
                }
            }
        }
        ticketsByFaction.clear();
    }

    public void releaseFaction(UUID factionId) {
        Map<Integer, List<ForgeChunkManager.Ticket>> byDim = ticketsByFaction.remove(factionId);
        if (byDim == null) {
            return;
        }
        for (List<ForgeChunkManager.Ticket> tickets : byDim.values()) {
            for (ForgeChunkManager.Ticket ticket : tickets) {
                ForgeChunkManager.releaseTicket(ticket);
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

        HashMap<Integer, HashSet<DimChunkPos>> chunksByDim = new HashMap<Integer, HashSet<DimChunkPos>>();

        for (DimChunkPos forced : faction.forcedChunks) {
            chunksByDim.computeIfAbsent(forced.dim, dim -> new HashSet<DimChunkPos>()).add(forced);
        }
        for (DimBlockPos collectorPos : faction.islandCollectors) {
            DimChunkPos collectorChunk = collectorPos.toChunkPos();
            chunksByDim.computeIfAbsent(collectorChunk.dim, dim -> new HashSet<DimChunkPos>()).add(collectorChunk);
        }

        HashMap<Integer, List<ForgeChunkManager.Ticket>> ticketsByDim = ticketsByFaction.computeIfAbsent(faction.uuid, id -> new HashMap<Integer, List<ForgeChunkManager.Ticket>>());

        ArrayList<Integer> obsoleteDims = new ArrayList<Integer>();
        for (Integer dim : ticketsByDim.keySet()) {
            if (!chunksByDim.containsKey(dim) || chunksByDim.get(dim).isEmpty()) {
                releaseAll(ticketsByDim.get(dim));
                obsoleteDims.add(dim);
            }
        }
        for (Integer dim : obsoleteDims) {
            ticketsByDim.remove(dim);
        }

        int maxDepth = Math.max(1, ForgeChunkManager.getMaxChunkDepthFor(Tags.MODID));

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

            List<ForgeChunkManager.Ticket> tickets = ticketsByDim.computeIfAbsent(dim, id -> new ArrayList<ForgeChunkManager.Ticket>());

            int neededTickets = (chunks.size() + maxDepth - 1) / maxDepth;

            while (tickets.size() > neededTickets) {
                ForgeChunkManager.releaseTicket(tickets.remove(tickets.size() - 1));
            }
            while (tickets.size() < neededTickets) {
                ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(WarForgeMod.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
                if (ticket == null) {
                    WarForgeMod.LOGGER.warn("Failed to allocate chunk-loading ticket for faction {} in dimension {} ({} of {} tickets allocated)", faction.name, dim, tickets.size(), neededTickets);
                    break;
                }
                tickets.add(ticket);
            }

            if (tickets.isEmpty()) {
                ticketsByDim.remove(dim);
                continue;
            }

            for (ForgeChunkManager.Ticket ticket : tickets) {
                for (ChunkPos chunkPos : new HashSet<ChunkPos>(ticket.getChunkList())) {
                    ForgeChunkManager.unforceChunk(ticket, chunkPos);
                }
            }

            int ticketIndex = 0;
            int forcedOnCurrent = 0;
            for (DimChunkPos chunk : chunks) {
                if (forcedOnCurrent >= maxDepth) {
                    ticketIndex++;
                    forcedOnCurrent = 0;
                }
                if (ticketIndex >= tickets.size()) {
                    WarForgeMod.LOGGER.warn("Faction {} exceeded chunk-loading capacity in dimension {}; {} chunks could not be force-loaded", faction.name, dim, chunks.size() - (ticketIndex * maxDepth));
                    break;
                }
                ForgeChunkManager.forceChunk(tickets.get(ticketIndex), new ChunkPos(chunk.x, chunk.z));
                forcedOnCurrent++;
            }
        }

        if (ticketsByDim.isEmpty()) {
            ticketsByFaction.remove(faction.uuid);
        }
    }

    private void releaseAll(List<ForgeChunkManager.Ticket> tickets) {
        if (tickets == null) {
            return;
        }
        for (ForgeChunkManager.Ticket ticket : tickets) {
            ForgeChunkManager.releaseTicket(ticket);
        }
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        for (ForgeChunkManager.Ticket ticket : tickets) {
            ForgeChunkManager.releaseTicket(ticket);
        }
    }
}
