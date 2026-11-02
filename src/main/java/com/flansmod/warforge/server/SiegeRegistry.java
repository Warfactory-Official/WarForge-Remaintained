package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.faction.ChunkRadius;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Active sieges keyed by defending chunk, and read-only queries over them. */
public final class SiegeRegistry {
    public enum SiegeZone {NONE, WAR, SIEGED}

    /** Zone governing a chunk plus the defending faction of the siege that governs it. */
    public static final class SiegeZoneResult {
        public static final SiegeZoneResult NONE = new SiegeZoneResult(SiegeZone.NONE, null);
        public final SiegeZone zone;
        public final UUID defendingFaction;

        private SiegeZoneResult(SiegeZone zone, UUID defendingFaction) {
            this.zone = zone;
            this.defendingFaction = defendingFaction;
        }
    }

    private final HashMap<DimChunkPos, Siege> sieges = new HashMap<>();
    private final FactionRegistry factions;
    private final ClaimRegistry claims;

    SiegeRegistry(FactionRegistry factions, ClaimRegistry claims) {
        this.factions = factions;
        this.claims = claims;
    }

    public Map<DimChunkPos, Siege> entries() {
        return Collections.unmodifiableMap(sieges);
    }

    public Collection<Siege> all() {
        return Collections.unmodifiableCollection(sieges.values());
    }

    public Siege get(DimChunkPos defendingChunk) {
        return sieges.get(defendingChunk);
    }

    public boolean isEmpty() {
        return sieges.isEmpty();
    }

    void put(DimChunkPos defendingChunk, Siege siege) {
        sieges.put(defendingChunk, siege);
    }

    Siege remove(DimChunkPos defendingChunk) {
        return sieges.remove(defendingChunk);
    }

    void clear() {
        sieges.clear();
    }

    public int countAttacking(UUID factionId) {
        int count = 0;
        for (Siege siege : sieges.values()) {
            if (factionId.equals(siege.attackingFaction)) count++;
        }
        return count;
    }

    public int countDefending(UUID factionId) {
        int count = 0;
        for (Siege siege : sieges.values()) {
            if (factionId.equals(siege.defendingFaction)) count++;
        }
        return count;
    }

    public boolean isDefending(UUID factionId) {
        for (Siege siege : sieges.values()) {
            if (factionId.equals(siege.defendingFaction)) return true;
        }
        return false;
    }

    public Siege getActiveSiegeForFaction(UUID factionId) {
        if (!FactionIds.isValid(factionId)) {
            return null;
        }
        for (Siege siege : sieges.values()) {
            if (factionId.equals(siege.attackingFaction) || factionId.equals(siege.defendingFaction)) {
                return siege;
            }
        }
        return null;
    }

    public boolean isFactionInActiveSiege(UUID factionId) {
        return getActiveSiegeForFaction(factionId) != null;
    }

    /** Chunk is a siege target or holds one of its camps. */
    public boolean isSiegeInProgress(DimChunkPos chunk) {
        for (Map.Entry<DimChunkPos, Siege> kvp : sieges.entrySet()) {
            if (kvp.getKey().equals(chunk)) {
                return true;
            }
            for (DimBlockPos camp : kvp.getValue().attackingCamps) {
                if (camp != null && camp.toChunkPos().equals(chunk)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isChunkInBattleZone(DimChunkPos chunk) {
        for (Siege siege : sieges.values()) {
            if (siege.isChunkInBattleZone(chunk)) {
                return true;
            }
        }
        return false;
    }

    public boolean isChunkContested(DimChunkPos chunk) {
        return isSiegeInProgress(chunk) || isChunkInBattleZone(chunk);
    }

    /** Inner Sieged zone wins over the outer War zone. */
    public SiegeZoneResult getSiegeZone(DimChunkPos chunk) {
        UUID warDefender = null;
        for (Siege siege : sieges.values()) {
            if (siege.isChunkInSiegedZone(chunk)) {
                return new SiegeZoneResult(SiegeZone.SIEGED, siege.defendingFaction);
            }
            if (warDefender == null && siege.isChunkInBattleZone(chunk)) {
                warDefender = siege.defendingFaction;
            }
        }
        return warDefender == null ? SiegeZoneResult.NONE : new SiegeZoneResult(SiegeZone.WAR, warDefender);
    }

    /** Within the faction's presence radius of a camp of a siege it attacks or defends. */
    public boolean isInOwnSiegeWarzone(UUID factionId, DimChunkPos pos) {
        if (!FactionIds.isValid(factionId)) {
            return false;
        }
        for (Siege siege : sieges.values()) {
            boolean attacker = factionId.equals(siege.attackingFaction);
            if (!attacker && !factionId.equals(siege.defendingFaction)) {
                continue;
            }
            int radius = attacker ? WarForgeConfig.SIEGE_ATTACKER_RADIUS : WarForgeConfig.SIEGE_DEFENDER_RADIUS;
            for (DimBlockPos camp : siege.attackingCamps) {
                if (camp != null && ChunkRadius.contains(camp.toChunkPos(), pos, radius)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateAttackerPresence(ServerPlayer player) {
        if (sieges.isEmpty()) {
            return;
        }
        UUID playerId = player.getUUID();
        Faction faction = factions.getFactionOfPlayer(playerId);
        UUID factionId = faction == null ? null : faction.getUuid();
        DimChunkPos chunk = new DimChunkPos(player.level().dimension(), player.blockPosition());
        for (Siege siege : sieges.values()) {
            siege.setAttackerPresent(playerId,
                    factionId != null && factionId.equals(siege.attackingFaction)
                            && siege.isChunkInAttackerPresenceZone(chunk, claims));
        }
    }

    public void removeAttackerPresence(UUID playerId) {
        for (Siege siege : sieges.values()) {
            siege.setAttackerPresent(playerId, false);
        }
    }

    /** Cardinal neighbours of {@code pos} claimed by another faction within vertical siege range. */
    public List<DimChunkPos> getAdjacentClaims(UUID excludingFaction, DimBlockPos pos) {
        ArrayList<DimChunkPos> targets = new ArrayList<>(4);
        DimChunkPos origin = pos.toChunkPos();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            DimChunkPos target = origin.offset(facing, 1);
            if (isSiegeable(excludingFaction, pos.getY(), target)) {
                targets.add(target);
            }
        }
        return targets;
    }

    /** Every chunk within {@code radius} of {@code originPos}, mapped to whether it is a valid siege target. */
    public LinkedHashMap<DimChunkPos, Boolean> getClaimRadiusAround(UUID excludedFaction, DimBlockPos originPos, int radius) {
        LinkedHashMap<DimChunkPos, Boolean> result = new LinkedHashMap<>();
        DimChunkPos center = originPos.toChunkPos();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                DimChunkPos chunk = new DimChunkPos(center.dim, x, z);
                result.put(chunk, isSiegeable(excludedFaction, originPos.getY(), chunk));
            }
        }
        return result;
    }

    private boolean isSiegeable(UUID attackingFaction, int originY, DimChunkPos target) {
        UUID ownerId = claims.getClaim(target);
        if (ownerId.equals(FactionIds.NONE) || ownerId.equals(attackingFaction)) {
            return false;
        }
        Faction owner = factions.getFaction(ownerId);
        DimBlockPos targetPos = owner == null ? null : owner.getSpecificPosForClaim(target);
        return targetPos != null && Math.abs(originY - targetPos.getY()) <= WarForgeConfig.VERTICAL_SIEGE_DIST;
    }
}
