package com.flansmod.warforge.server;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.init.VeinUtils;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketJourneyMapVeins;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side driver for the optional JourneyMap vein-icon overlay.
 * <p>
 * Server-authoritative: every vein shown is computed by the real {@link VeinUtils} handler, so it always
 * matches yields and administrator overrides. Modes (from {@link WarForgeConfig#JOURNEYMAP_VEIN_MODE}):
 * <ul>
 *   <li><b>DISABLED</b>: nothing is sent.</li>
 *   <li><b>PLAYER</b>: a chunk's vein is sent only once the player walks into and loads it.</li>
 *   <li><b>AUTO</b>: veins for a wide radius around each player are streamed automatically as they move.</li>
 * </ul>
 * In every mode the server only ever sends chunks the player is entitled to under that mode, so a client
 * cannot pull veins it has not legitimately reached.
 */
public class JourneyMapVeinSync {
    private static final int TICK_INTERVAL = 10;          // run AUTO scans ~twice a second
    private static final int MAX_CHUNKS_PER_SCAN = 256;   // throttle vein generation per player per scan
    private static final int MAX_ENTRIES_PER_PACKET = 1000;

    private int tickCounter = 0;
    // What vein short we have actually sent each player, per chunk (markers only; null veins are not stored).
    private final Map<UUID, Map<DimChunkPos, Short>> playerViews = new HashMap<>();
    // AUTO per-player rolling scan state.
    private final Map<UUID, AutoState> autoStates = new HashMap<>();

    private static final class AutoState {
        int dim, centerX, centerZ;
        int cursor;
        boolean complete;
    }

    private static int mode() {
        return WarForgeConfig.JOURNEYMAP_VEIN_MODE;
    }

    private static long seedForDim(int dim) {
        MinecraftServer server = WarForgeMod.MC_SERVER;
        if (server == null) {
            return 0L;
        }
        World world = server.getWorld(dim);
        if (world == null) {
            world = server.getWorld(0);
        }
        return world == null ? 0L : world.provider.getSeed();
    }

    /** The compressed vein short at a chunk, or {@code null} if there is no vein (so no marker). */
    private Short veinInfoAt(int dim, int x, int z) {
        VeinUtils handler = WarForgeMod.VEIN_HANDLER;
        if (handler == null || !handler.hasFinishedInit || !handler.dimHasVeins(dim)) {
            return null;
        }
        Pair<Vein, Quality> info = handler.getVein(dim, x, z, seedForDim(dim));
        if (info == null) {
            return null;
        }
        short compressed = handler.compressVeinInfo(info);
        return compressed == VeinUtils.NULL_VEIN_ID ? null : compressed;
    }

    public void tick() {
        if (mode() != WarForgeConfig.JM_MODE_AUTO) {
            return;
        }
        if (++tickCounter < TICK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = WarForgeMod.MC_SERVER;
        if (server == null) {
            return;
        }
        int radius = Math.max(1, WarForgeConfig.JOURNEYMAP_VEIN_AUTO_RADIUS);
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            scanAuto(player, radius);
        }
    }

    private void scanAuto(EntityPlayerMP player, int radius) {
        AutoState state = autoStates.get(player.getUniqueID());
        if (state == null || state.dim != player.dimension || state.centerX != player.chunkCoordX || state.centerZ != player.chunkCoordZ) {
            state = new AutoState();
            state.dim = player.dimension;
            state.centerX = player.chunkCoordX;
            state.centerZ = player.chunkCoordZ;
            state.cursor = 0;
            state.complete = false;
            autoStates.put(player.getUniqueID(), state);
        }
        if (state.complete) {
            return;
        }

        int side = 2 * radius + 1;
        int total = side * side;
        PacketJourneyMapVeins packet = new PacketJourneyMapVeins();
        int processed = 0;
        while (state.cursor < total && processed < MAX_CHUNKS_PER_SCAN) {
            int idx = state.cursor++;
            int x = state.centerX + (idx % side) - radius;
            int z = state.centerZ + (idx / side) - radius;
            accumulate(packet, player.getUniqueID(), new DimChunkPos(state.dim, x, z), veinInfoAt(state.dim, x, z));
            processed++;
            if (packet.sets.size() + packet.removes.size() >= MAX_ENTRIES_PER_PACKET) {
                WarForgeMod.NETWORK.sendTo(packet, player);
                packet = new PacketJourneyMapVeins();
            }
        }
        if (!packet.isEmpty()) {
            WarForgeMod.NETWORK.sendTo(packet, player);
        }
        if (state.cursor >= total) {
            state.complete = true;
        }
    }

    /** PLAYER mode: the player just loaded this chunk, so reveal its vein to them. */
    public void onChunkObserved(EntityPlayerMP player, int dim, int x, int z) {
        if (mode() != WarForgeConfig.JM_MODE_PLAYER) {
            return;
        }
        sendSingle(player, new DimChunkPos(dim, x, z), veinInfoAt(dim, x, z));
    }

    /** Called when an administrator changes a chunk's vein so observers see it update. */
    public void onVeinChanged(int dim, int x, int z) {
        int mode = mode();
        if (mode == WarForgeConfig.JM_MODE_DISABLED) {
            return;
        }
        MinecraftServer server = WarForgeMod.MC_SERVER;
        if (server == null) {
            return;
        }
        DimChunkPos chunk = new DimChunkPos(dim, x, z);
        int radius = Math.max(1, WarForgeConfig.JOURNEYMAP_VEIN_AUTO_RADIUS);
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player.dimension != dim) {
                continue;
            }
            Map<DimChunkPos, Short> view = playerViews.get(player.getUniqueID());
            if (view != null && view.containsKey(chunk)) {
                sendSingle(player, chunk, veinInfoAt(dim, x, z));
            }
            // AUTO: force a re-scan so a newly-added vein on a previously-empty chunk gets picked up.
            if (mode == WarForgeConfig.JM_MODE_AUTO) {
                AutoState state = autoStates.get(player.getUniqueID());
                if (state != null && state.dim == dim
                        && Math.abs(player.chunkCoordX - x) <= radius && Math.abs(player.chunkCoordZ - z) <= radius) {
                    state.complete = false;
                    state.cursor = 0;
                }
            }
        }
    }

    public void onPlayerJoin(EntityPlayerMP player) {
        playerViews.remove(player.getUniqueID());
        autoStates.remove(player.getUniqueID());
    }

    public void onPlayerLogout(EntityPlayerMP player) {
        playerViews.remove(player.getUniqueID());
        autoStates.remove(player.getUniqueID());
    }

    private void sendSingle(EntityPlayerMP player, DimChunkPos chunk, Short veinInfo) {
        PacketJourneyMapVeins packet = new PacketJourneyMapVeins();
        accumulate(packet, player.getUniqueID(), chunk, veinInfo);
        if (!packet.isEmpty()) {
            WarForgeMod.NETWORK.sendTo(packet, player);
        }
    }

    private void accumulate(PacketJourneyMapVeins packet, UUID playerId, DimChunkPos chunk, Short veinInfo) {
        Map<DimChunkPos, Short> view = playerViews.computeIfAbsent(playerId, id -> new HashMap<>());
        Short sent = view.get(chunk);
        if (veinInfo != null) {
            if (veinInfo.equals(sent)) {
                return;
            }
            packet.addSet(chunk.dim, chunk.x, chunk.z, veinInfo);
            view.put(chunk, veinInfo);
        } else {
            if (sent == null) {
                return;
            }
            packet.addRemove(chunk.dim, chunk.x, chunk.z);
            view.remove(chunk);
        }
    }
}
