package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.TimeHelper;
import com.flansmod.warforge.compat.Keys;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

public final class ClaimPolicy {
    private final FactionStorage hub;

    ClaimPolicy(FactionStorage hub) {
        this.hub = hub;
    }

    public static boolean isDimWhitelisted(ResourceKey<Level> dim) {
        String id = Keys.id(dim).toString();
        for (String allowed : WarForgeConfig.CLAIM_DIM_WHITELIST) {
            if (allowed.equals(id)) {
                return true;
            }
        }
        return false;
    }

    // lazy island lookup per faction; reuse across chunk checks; invalid after any claim change
    public final class IslandCache {
        private final Faction faction;
        private final HashMap<DimChunkPos, Integer> islandOf = new HashMap<>();
        private final HashSet<Integer> islandsWithCollector = new HashSet<>();
        private boolean built;

        private IslandCache(Faction faction) {
            this.faction = faction;
        }

        private void build() {
            built = true;
            int nextId = 0;
            for (DimBlockPos pos : faction.getClaimPositions()) {
                DimChunkPos start = pos.toChunkPos();
                if (islandOf.containsKey(start)) {
                    continue;
                }
                int id = nextId++;
                ArrayDeque<DimChunkPos> queue = new ArrayDeque<>();
                queue.add(start);
                islandOf.put(start, id);
                while (!queue.isEmpty()) {
                    DimChunkPos current = queue.poll();
                    for (Direction facing : Direction.Plane.HORIZONTAL) {
                        DimChunkPos neighbor = current.offset(facing, 1);
                        if (faction.getUuid().equals(hub.claims.getClaim(neighbor)) && islandOf.putIfAbsent(neighbor, id) == null) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
            for (DimBlockPos collector : faction.getIslandCollectors()) {
                Integer id = islandOf.get(collector.toChunkPos());
                if (id != null) {
                    islandsWithCollector.add(id);
                }
            }
        }

        /** Claiming {@code chunk} would join two or more islands that each hold a collector. */
        public boolean wouldMergeCollectorIslands(DimChunkPos chunk) {
            if (!built) {
                build();
            }
            Integer first = null;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                Integer id = islandOf.get(chunk.offset(facing, 1));
                if (id == null || !islandsWithCollector.contains(id)) {
                    continue;
                }
                if (first == null) {
                    first = id;
                } else if (!first.equals(id)) {
                    return true;
                }
            }
            return false;
        }
    }

    public IslandCache islandCache(Faction faction) {
        return new IslandCache(faction);
    }

    public DimChunkPos findAdjacentOwned(DimChunkPos chunk, UUID factionId) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            DimChunkPos adjacent = chunk.offset(facing, 1);
            if (factionId.equals(hub.claims.getClaim(adjacent))) {
                return adjacent;
            }
        }
        return null;
    }

    /**
     * Opposing (not self, ally or zone) faction with a claim within MIN_DISTANCE_BETWEEN_FACTIONS chunks
     * (Chebyshev) of {@code chunk}; {@code faction} null = founding, every other faction opposes.
     */
    public Faction findNearbyOpposingFaction(Faction faction, DimChunkPos chunk) {
        int distance = WarForgeConfig.MIN_DISTANCE_BETWEEN_FACTIONS;
        if (distance <= 0) {
            return null;
        }
        UUID selfId = faction == null ? FactionIds.NONE : faction.getUuid();
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dz = -distance; dz <= distance; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                UUID owner = hub.claims.getClaim(new DimChunkPos(chunk.dim, chunk.x + dx, chunk.z + dz));
                if (owner.equals(FactionIds.NONE) || owner.equals(selfId) || FactionIds.isNeutralZone(owner)
                        || (faction != null && faction.isAllyOf(owner))) {
                    continue;
                }
                Faction opposing = hub.registry.getFaction(owner);
                if (opposing != null) {
                    return opposing;
                }
            }
        }
        return null;
    }

    /** Rules on the chunk itself, shared by founding, claiming and siege camps; heals an orphaned index entry on the way. */
    public Component checkChunkFree(DimChunkPos chunk) {
        hub.claims.pruneStaleClaim(chunk);
        if (hub.claims.isClaimed(chunk)) {
            return t("claim.already_claimed");
        }
        if (hub.sieges.isChunkContested(chunk)) {
            return t("claim.contested");
        }
        if (hub.conquered.isConquered(chunk)) {
            return t("claim.conquered", TimeHelper.formatTime(hub.conquered.getRemainingMs(chunk)));
        }
        if (!isDimWhitelisted(chunk.dim)) {
            return t("claim.bad_dimension");
        }
        if (WarForgeMod.FOBS.isNearActiveFob(chunk, WarForgeConfig.FOB_CLAIM_EXCLUSION_RADIUS)) {
            return t("claim.near_fob", WarForgeConfig.FOB_CLAIM_EXCLUSION_RADIUS);
        }
        return null;
    }

    public Component checkFoundFaction(Player player, DimChunkPos chunk) {
        if (hub.registry.getFactionOfPlayer(player.getUUID()) != null) {
            return t("faction.already_member");
        }
        Component denial = checkChunkFree(chunk);
        if (denial != null) {
            return denial;
        }
        Faction tooClose = findNearbyOpposingFaction(null, chunk);
        return tooClose == null ? null : t("claim.found_too_close", WarForgeConfig.MIN_DISTANCE_BETWEEN_FACTIONS, tooClose.getName());
    }

    /** @param islands null = build one for this call */
    public Component checkClaim(Player player, Faction faction, DimChunkPos chunk, IslandCache islands) {
        if (!WarForgeMod.isOp(player) && !faction.isPlayerRoleInFaction(player.getUUID(), Role.OFFICER)) {
            return t("role.officer_required");
        }
        Component denial = checkChunkFree(chunk);
        if (denial != null) {
            return denial;
        }
        if (!faction.canPlaceClaim()) {
            return t(WarForgeConfig.ENABLE_CITADEL_UPGRADES ? "claim.limit_level" : "claim.limit");
        }
        if (!WarForgeConfig.ENABLE_ISOLATED_CLAIMS && findAdjacentOwned(chunk, faction.getUuid()) == null) {
            return t("claim.isolated");
        }
        Faction tooClose = findNearbyOpposingFaction(faction, chunk);
        if (tooClose != null) {
            return t("claim.too_close", WarForgeConfig.MIN_DISTANCE_BETWEEN_FACTIONS, tooClose.getName());
        }
        IslandCache cache = islands != null ? islands : new IslandCache(faction);
        if (cache.wouldMergeCollectorIslands(chunk)) {
            return t("claim.merges_collectors");
        }
        return null;
    }

    public boolean canClaim(Player player, Faction faction, DimChunkPos chunk, IslandCache islands) {
        return checkClaim(player, faction, chunk, islands) == null;
    }
}
