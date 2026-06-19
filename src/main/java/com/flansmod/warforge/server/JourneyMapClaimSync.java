package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketJourneyMapClaims;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side driver for the optional JourneyMap claim-border overlay.
 * <p>
 * Three modes, chosen by the server-authoritative {@link WarForgeConfig#JOURNEYMAP_CLAIM_MODE}:
 * <ul>
 *   <li><b>DISABLED</b>: nothing is ever sent.</li>
 *   <li><b>AUTO</b>: every claim is broadcast to every client, updated live.</li>
 *   <li><b>PLAYER</b>: a claim is only sent to a client once that client observes (watches) the chunk,
 *       and live changes only reach clients currently watching the affected chunk. Unobserved claims
 *       are never transmitted, so a client cannot learn associations it has not legitimately seen.</li>
 * </ul>
 * The payload is colour + position only, never faction identity.
 */
public class JourneyMapClaimSync {
    private static final int TICK_INTERVAL = 40; // run the diff roughly every 2 seconds
    private static final int MAX_ENTRIES_PER_PACKET = 1000; // keep packets well under the custom-payload size limit

    private int tickCounter = 0;
    // Last global claim state we diffed against (chunk -> faction colour).
    private final Map<DimChunkPos, Integer> lastGlobal = new HashMap<>();
    // PLAYER mode: what we have actually told each player (chunk -> colour), so we can sync precisely on observation.
    private final Map<UUID, Map<DimChunkPos, Integer>> playerViews = new HashMap<>();

    private static int mode() {
        return WarForgeConfig.JOURNEYMAP_CLAIM_MODE;
    }

    /** Snapshot of every current claim mapped to its faction colour. */
    private Map<DimChunkPos, Integer> currentClaimColours() {
        Map<DimChunkPos, Integer> result = new HashMap<>();
        for (Map.Entry<DimChunkPos, UUID> entry : WarForgeMod.FACTIONS.getClaims().entrySet()) {
            Integer colour = colourOf(entry.getValue());
            if (colour != null) {
                result.put(entry.getKey(), colour);
            }
        }
        return result;
    }

    private Integer colourOf(UUID factionId) {
        if (factionId == null || factionId.equals(Faction.nullUuid)) {
            return null;
        }
        Faction faction = WarForgeMod.FACTIONS.getFaction(factionId);
        return faction == null ? null : faction.colour;
    }

    /** Called every server tick; throttles itself to {@link #TICK_INTERVAL}. */
    public void tick() {
        int mode = mode();
        if (mode == WarForgeConfig.JM_MODE_DISABLED) {
            if (!lastGlobal.isEmpty()) {
                lastGlobal.clear();
            }
            return;
        }
        if (++tickCounter < TICK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        Map<DimChunkPos, Integer> current = currentClaimColours();
        List<DimChunkPos> changed = new ArrayList<>();
        List<DimChunkPos> removed = new ArrayList<>();
        for (Map.Entry<DimChunkPos, Integer> entry : current.entrySet()) {
            Integer prev = lastGlobal.get(entry.getKey());
            if (prev == null || !prev.equals(entry.getValue())) {
                changed.add(entry.getKey());
            }
        }
        for (DimChunkPos chunk : lastGlobal.keySet()) {
            if (!current.containsKey(chunk)) {
                removed.add(chunk);
            }
        }

        if (!changed.isEmpty() || !removed.isEmpty()) {
            if (mode == WarForgeConfig.JM_MODE_AUTO) {
                broadcastAuto(changed, removed, current);
            } else { // PLAYER: only push changes to clients currently observing the affected chunk
                for (DimChunkPos chunk : changed) {
                    syncChunkToWatchers(chunk, current.get(chunk));
                }
                for (DimChunkPos chunk : removed) {
                    syncChunkToWatchers(chunk, null);
                }
            }
        }

        lastGlobal.clear();
        lastGlobal.putAll(current);
    }

    /** AUTO: hand a joining player the full current claim set immediately. PLAYER: wait for observation. */
    public void onPlayerJoin(EntityPlayerMP player) {
        if (mode() != WarForgeConfig.JM_MODE_AUTO) {
            return;
        }
        List<Map.Entry<DimChunkPos, Integer>> entries = new ArrayList<>(currentClaimColours().entrySet());
        int index = 0;
        boolean firstPacket = true;
        // The first packet clears the client cache; at least one packet is always sent.
        do {
            PacketJourneyMapClaims packet = new PacketJourneyMapClaims();
            packet.clear = firstPacket;
            int end = Math.min(index + MAX_ENTRIES_PER_PACKET, entries.size());
            for (; index < end; index++) {
                DimChunkPos chunk = entries.get(index).getKey();
                packet.addSet(chunk.dim, chunk.x, chunk.z, entries.get(index).getValue());
            }
            WarForgeMod.NETWORK.sendTo(packet, player);
            firstPacket = false;
        } while (index < entries.size());
    }

    private void broadcastAuto(List<DimChunkPos> changed, List<DimChunkPos> removed, Map<DimChunkPos, Integer> current) {
        PacketJourneyMapClaims packet = new PacketJourneyMapClaims();
        int count = 0;
        for (DimChunkPos chunk : changed) {
            packet.addSet(chunk.dim, chunk.x, chunk.z, current.get(chunk));
            if (++count >= MAX_ENTRIES_PER_PACKET) {
                WarForgeMod.NETWORK.sendToAll(packet);
                packet = new PacketJourneyMapClaims();
                count = 0;
            }
        }
        for (DimChunkPos chunk : removed) {
            packet.addRemove(chunk.dim, chunk.x, chunk.z);
            if (++count >= MAX_ENTRIES_PER_PACKET) {
                WarForgeMod.NETWORK.sendToAll(packet);
                packet = new PacketJourneyMapClaims();
                count = 0;
            }
        }
        if (!packet.isEmpty()) {
            WarForgeMod.NETWORK.sendToAll(packet);
        }
    }

    public void onPlayerLogout(EntityPlayerMP player) {
        playerViews.remove(player.getUniqueID());
    }

    /** PLAYER mode: a client just started watching this chunk, so bring its view of that chunk up to date. */
    public void onChunkObserved(EntityPlayerMP player, int dim, int chunkX, int chunkZ) {
        if (mode() != WarForgeConfig.JM_MODE_PLAYER) {
            return;
        }
        DimChunkPos chunk = new DimChunkPos(dim, chunkX, chunkZ);
        syncChunkToPlayer(player, chunk, colourOf(WarForgeMod.FACTIONS.getClaims().get(chunk)));
    }

    private void syncChunkToWatchers(DimChunkPos chunk, Integer colour) {
        MinecraftServer server = WarForgeMod.MC_SERVER;
        if (server == null) {
            return;
        }
        int viewDistance = server.getPlayerList().getViewDistance();
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player.dimension != chunk.dim) {
                continue;
            }
            int dx = Math.abs(player.chunkCoordX - chunk.x);
            int dz = Math.abs(player.chunkCoordZ - chunk.z);
            if (Math.max(dx, dz) <= viewDistance) { // within view => actually observing the chunk
                syncChunkToPlayer(player, chunk, colour);
            }
        }
    }

    private void syncChunkToPlayer(EntityPlayerMP player, DimChunkPos chunk, Integer colour) {
        Map<DimChunkPos, Integer> view = playerViews.computeIfAbsent(player.getUniqueID(), id -> new HashMap<>());
        Integer sent = view.get(chunk);
        PacketJourneyMapClaims packet = new PacketJourneyMapClaims();
        if (colour != null) {
            if (colour.equals(sent)) {
                return; // already in sync
            }
            packet.addSet(chunk.dim, chunk.x, chunk.z, colour);
            view.put(chunk, colour);
        } else {
            if (sent == null) {
                return; // nothing to clear
            }
            packet.addRemove(chunk.dim, chunk.x, chunk.z);
            view.remove(chunk);
        }
        WarForgeMod.NETWORK.sendTo(packet, player);
    }
}
