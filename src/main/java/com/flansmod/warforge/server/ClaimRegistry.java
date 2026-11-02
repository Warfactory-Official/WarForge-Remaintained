package com.flansmod.warforge.server;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Sole mutator of chunk->owner index: claim/unclaim keep index, faction claims, wealth, force-loads, collectors consistent.
public final class ClaimRegistry {
    private final HashMap<DimChunkPos, UUID> owners = new HashMap<>();
    /** Bumped on every ownership change; keys caches derived from the index. */
    @Getter
    private int revision;

    public Map<DimChunkPos, UUID> getClaims() {
        return Collections.unmodifiableMap(owners);
    }

    /** @return {@link FactionIds#NONE} if unclaimed */
    public UUID getClaim(DimChunkPos chunk) {
        UUID owner = owners.get(chunk);
        return owner == null ? FactionIds.NONE : owner;
    }

    public UUID getClaim(DimBlockPos pos) {
        return getClaim(pos.toChunkPos());
    }

    public boolean isClaimed(DimChunkPos chunk) {
        return owners.containsKey(chunk);
    }

    public boolean isClaimedByOther(UUID excludingFaction, DimChunkPos chunk) {
        UUID owner = owners.get(chunk);
        return owner != null && !owner.equals(excludingFaction);
    }

    public boolean hasClaimWithin(DimChunkPos center, int radius) {
        if (radius < 0) {
            return false;
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (owners.containsKey(new DimChunkPos(center.dim, center.x + dx, center.z + dz))) {
                    return true;
                }
            }
        }
        return false;
    }

    void claim(Faction faction, DimBlockPos pos, ClaimType type) {
        owners.put(pos.toChunkPos(), faction.getUuid());
        revision++;
        faction.putClaim(pos, type, 0);
        faction.addWealth(chunkWealth(pos.toChunkPos()));
    }

    DimBlockPos claimData(Faction faction, DimChunkPos chunk, int y, ClaimType type) {
        DimBlockPos pos = new DimBlockPos(chunk.dim, chunk.getMinBlockX(), y, chunk.getMinBlockZ());
        claim(faction, pos, type);
        return pos;
    }

    // citadel claim not removable here
    void unclaim(Faction faction, DimBlockPos pos) {
        DimChunkPos chunk = pos.toChunkPos();
        boolean wasForced = faction.unforceLoad(chunk);
        faction.removeClaim(pos);
        owners.remove(chunk);
        revision++;
        faction.addWealth(-chunkWealth(chunk));

        ArrayList<DimBlockPos> removedCollectors = new ArrayList<>();
        for (DimBlockPos collectorPos : faction.getIslandCollectors()) {
            if (collectorPos.toChunkPos().equals(chunk)) {
                removedCollectors.add(collectorPos);
                ServerLevel world = WarForgeMod.MC_SERVER.getLevel(collectorPos.dim);
                if (world != null) {
                    world.removeBlock(collectorPos.toRegularPos(), false);
                }
            }
        }
        faction.removeIslandCollectors(removedCollectors);
        if (wasForced || !removedCollectors.isEmpty()) {
            WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        }
    }

    void moveCitadel(Faction faction, DimBlockPos newPos) {
        DimBlockPos oldPos = faction.getCitadelPos();
        DimChunkPos oldChunk = oldPos.toChunkPos();
        DimChunkPos newChunk = newPos.toChunkPos();
        DimBlockPos replaced = newChunk.equals(oldChunk) ? oldPos : faction.getSpecificPosForClaim(newChunk);
        ClaimType replacedType = faction.getClaimType(newChunk);

        int oldPending = faction.removeClaim(oldPos);
        int targetPending = replaced.equals(oldPos) ? oldPending : faction.removeClaim(replaced);
        if (!newChunk.equals(oldChunk)) {
            faction.putClaim(oldPos, replacedType, oldPending);
        }
        faction.setCitadelPos(newPos);
        faction.putClaim(newPos, ClaimType.CITADEL, targetPending);
    }

    public void pruneStaleClaim(DimChunkPos chunk) {
        UUID owner = owners.get(chunk);
        if (owner == null) {
            return;
        }
        Faction faction = WarForgeMod.FACTIONS.registry.getFaction(owner);
        if (faction == null || faction.getSpecificPosForClaim(chunk) == null) {
            owners.remove(chunk);
            revision++;
        }
    }

    void releaseAll(Faction faction) {
        revision++;
        for (DimBlockPos pos : faction.getClaimPositions()) {
            owners.remove(pos.toChunkPos(), faction.getUuid());
        }
    }

    void clear() {
        revision++;
        owners.clear();
    }

    void rebuild(Collection<Faction> factions) {
        revision++;
        owners.clear();
        for (Faction faction : factions) {
            for (DimBlockPos pos : faction.getClaimPositions()) {
                owners.put(pos.toChunkPos(), faction.getUuid());
            }
        }
    }

    /** Wealth of the vein under a chunk; 0 until the vein handler is ready (startup recompute covers it). */
    public static int chunkWealth(DimChunkPos chunk) {
        var veinHandler = WarForgeMod.VEIN_HANDLER;
        if (veinHandler == null || !veinHandler.hasFinishedInit || WarForgeMod.MC_SERVER == null) {
            return 0;
        }
        ServerLevel level = WarForgeMod.MC_SERVER.getLevel(chunk.dim);
        if (level == null) {
            level = WarForgeMod.MC_SERVER.overworld();
        }
        Pair<Vein, Quality> veinInfo = veinHandler.getVein(chunk.dim, chunk.x, chunk.z, level.getSeed());
        return veinInfo == null || veinInfo.getLeft() == null ? 0 : veinInfo.getLeft().wealth;
    }

    /** Full recompute (startup, vein reload); runtime changes adjust wealth incrementally. */
    void recalculateWealth(Faction faction) {
        int total = 0;
        for (DimBlockPos pos : faction.getClaimPositions()) {
            total += chunkWealth(pos.toChunkPos());
        }
        faction.setWealth(total);
    }

    public Set<DimChunkPos> collectIsland(UUID factionId, DimChunkPos start) {
        HashSet<DimChunkPos> visited = new HashSet<>();
        if (!factionId.equals(owners.get(start))) {
            return visited;
        }
        ArrayDeque<DimChunkPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            DimChunkPos current = queue.poll();
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                DimChunkPos neighbor = current.offset(facing, 1);
                if (factionId.equals(owners.get(neighbor)) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }
}
