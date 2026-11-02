package com.flansmod.warforge.server;

import com.flansmod.warforge.compat.Chunks;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.VeinCodec;
import com.flansmod.warforge.server.vein.VeinUtils;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketJourneyMapVeins;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Server-authoritative: only chunks the player is entitled to under the mode are sent (no client-side vein pull).
public class JourneyMapVeinSync {
    private static final int TICK_INTERVAL = 10;          // run AUTO scans ~twice a second
    private static final int MAX_CHUNKS_PER_SCAN = 256;   // throttle vein generation per player per scan
    private static final int MAX_ENTRIES_PER_PACKET = 1000;

    private int tickCounter = 0;
    private final Map<UUID, Map<DimChunkPos, Short>> playerViews = new HashMap<>();
    private final Map<UUID, AutoState> autoStates = new HashMap<>();

    private static final class AutoState {
        ResourceKey<Level> dim;
        int centerX, centerZ;
        int cursor;
        boolean complete;
    }

    private static int mode() {
        return WarForgeConfig.JOURNEYMAP_VEIN_MODE;
    }

    private static long seedForDim(ResourceKey<Level> dim) {
        MinecraftServer server = WarForgeMod.MC_SERVER;
        if (server == null) {
            return 0L;
        }
        // All dimensions of a world share the same world seed; fall back to the overworld if the dim is unloaded.
        ServerLevel level = server.getLevel(dim);
        if (level == null) {
            level = server.overworld();
        }
        return level == null ? 0L : level.getSeed();
    }

    private Short veinInfoAt(ResourceKey<Level> dim, int x, int z) {
        VeinUtils handler = WarForgeMod.VEIN_HANDLER;
        if (handler == null || !handler.hasFinishedInit || !handler.dimHasVeins(dim)) {
            return null;
        }
        Pair<Vein, Quality> info = handler.getVein(dim, x, z, seedForDim(dim));
        if (info == null) {
            return null;
        }
        short compressed = VeinCodec.compressVeinInfo(info);
        return compressed == VeinCodec.NULL_VEIN_ID ? null : compressed;
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            scanAuto(player, radius);
        }
    }

    private void scanAuto(ServerPlayer player, int radius) {
        ResourceKey<Level> dim = player.level().dimension();
        int chunkX = Chunks.x(player.chunkPosition());
        int chunkZ = Chunks.z(player.chunkPosition());
        AutoState state = autoStates.get(player.getUUID());
        if (state == null || !state.dim.equals(dim) || state.centerX != chunkX || state.centerZ != chunkZ) {
            state = new AutoState();
            state.dim = dim;
            state.centerX = chunkX;
            state.centerZ = chunkZ;
            state.cursor = 0;
            state.complete = false;
            autoStates.put(player.getUUID(), state);
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
            accumulate(packet, player.getUUID(), new DimChunkPos(state.dim, x, z), veinInfoAt(state.dim, x, z));
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

    public void onChunkObserved(ServerPlayer player, ResourceKey<Level> dim, int x, int z) {
        if (mode() != WarForgeConfig.JM_MODE_PLAYER) {
            return;
        }
        sendSingle(player, new DimChunkPos(dim, x, z), veinInfoAt(dim, x, z));
    }

    public void onVeinChanged(ResourceKey<Level> dim, int x, int z) {
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(dim)) {
                continue;
            }
            Map<DimChunkPos, Short> view = playerViews.get(player.getUUID());
            if (view != null && view.containsKey(chunk)) {
                sendSingle(player, chunk, veinInfoAt(dim, x, z));
            }
            // AUTO: force a re-scan so a newly-added vein on a previously-empty chunk gets picked up.
            if (mode == WarForgeConfig.JM_MODE_AUTO) {
                AutoState state = autoStates.get(player.getUUID());
                if (state != null && state.dim.equals(dim)
                        && Math.abs(Chunks.x(player.chunkPosition()) - x) <= radius && Math.abs(Chunks.z(player.chunkPosition()) - z) <= radius) {
                    state.complete = false;
                    state.cursor = 0;
                }
            }
        }
    }

    public void onPlayerJoin(ServerPlayer player) {
        playerViews.remove(player.getUUID());
        autoStates.remove(player.getUUID());
    }

    public void onPlayerLogout(ServerPlayer player) {
        playerViews.remove(player.getUUID());
        autoStates.remove(player.getUUID());
    }

    private void sendSingle(ServerPlayer player, DimChunkPos chunk, Short veinInfo) {
        PacketJourneyMapVeins packet = new PacketJourneyMapVeins();
        accumulate(packet, player.getUUID(), chunk, veinInfo);
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
