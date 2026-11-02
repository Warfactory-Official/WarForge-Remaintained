package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.PacketClaimMapDelta;
import com.flansmod.warforge.common.util.DimChunkPos;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Pushes every claim of the world (owner and colour only) to each player for map overlays, when
 * {@link WarForgeConfig#CLAIM_MAP_WHOLE_WORLD} is on: the full set on login, deltas on claim, unclaim and recolour.
 */
public final class ClaimMapWorldSync {
    private static final int POLL_TICKS = 40;
    /** Recolours do not bump the claim revision, so they are found by a periodic full diff. */
    private static final int FORCED_DIFF_POLLS = 15;

    private final FactionStorage hub;
    private final Map<DimChunkPos, UUID> lastOwner = new HashMap<>();
    private final Object2IntOpenHashMap<DimChunkPos> lastColour = new Object2IntOpenHashMap<>();
    private int ticks;
    private int polls;
    private int lastRevision = -1;

    ClaimMapWorldSync(FactionStorage hub) {
        this.hub = hub;
    }

    public void tick() {
        if (!WarForgeConfig.CLAIM_MAP_WHOLE_WORLD || ++ticks < POLL_TICKS) {
            return;
        }
        ticks = 0;
        int revision = hub.claims.getRevision();
        if (revision == lastRevision && ++polls < FORCED_DIFF_POLLS) {
            return;
        }
        polls = 0;
        lastRevision = revision;

        Map<DimChunkPos, UUID> current = hub.claims.getClaims();
        PacketClaimMapDelta delta = new PacketClaimMapDelta();
        for (Map.Entry<DimChunkPos, UUID> entry : current.entrySet()) {
            int colour = colourOf(entry.getValue());
            if (colour == NO_COLOUR) {
                continue;
            }
            UUID previous = lastOwner.get(entry.getKey());
            if (!entry.getValue().equals(previous) || lastColour.getInt(entry.getKey()) != colour) {
                lastOwner.put(entry.getKey(), entry.getValue());
                lastColour.put(entry.getKey(), colour);
                delta.sets.add(new PacketClaimMapDelta.Set(entry.getKey().dim, entry.getKey().x, entry.getKey().z, entry.getValue(), colour));
                delta = flushIfFull(delta);
            }
        }
        Iterator<DimChunkPos> known = lastOwner.keySet().iterator();
        while (known.hasNext()) {
            DimChunkPos chunk = known.next();
            if (colourOf(current.get(chunk)) == NO_COLOUR) {
                known.remove();
                lastColour.removeInt(chunk);
                delta.removes.add(new PacketClaimMapDelta.Remove(chunk.dim, chunk.x, chunk.z));
                delta = flushIfFull(delta);
            }
        }
        if (delta.size() > 0) {
            broadcast(delta);
        }
    }

    /** The whole current claim set, in batches. */
    public void sendAll(ServerPlayer player) {
        if (!WarForgeConfig.CLAIM_MAP_WHOLE_WORLD) {
            return;
        }
        PacketClaimMapDelta batch = new PacketClaimMapDelta();
        for (Map.Entry<DimChunkPos, UUID> entry : hub.claims.getClaims().entrySet()) {
            int colour = colourOf(entry.getValue());
            if (colour == NO_COLOUR) {
                continue;
            }
            batch.sets.add(new PacketClaimMapDelta.Set(entry.getKey().dim, entry.getKey().x, entry.getKey().z, entry.getValue(), colour));
            if (batch.isFull()) {
                WarForgeMod.NETWORK.sendTo(batch, player);
                batch = new PacketClaimMapDelta();
            }
        }
        if (batch.size() > 0) {
            WarForgeMod.NETWORK.sendTo(batch, player);
        }
    }

    private static final int NO_COLOUR = Integer.MIN_VALUE;

    private int colourOf(UUID owner) {
        if (owner == null || owner.equals(FactionIds.NONE)) {
            return NO_COLOUR;
        }
        Faction faction = hub.registry.getFaction(owner);
        return faction == null ? NO_COLOUR : faction.getColour();
    }

    private PacketClaimMapDelta flushIfFull(PacketClaimMapDelta delta) {
        if (!delta.isFull()) {
            return delta;
        }
        broadcast(delta);
        return new PacketClaimMapDelta();
    }

    private static void broadcast(PacketClaimMapDelta delta) {
        for (ServerPlayer player : WarForgeMod.MC_SERVER.getPlayerList().getPlayers()) {
            WarForgeMod.NETWORK.sendTo(delta, player);
        }
    }
}
