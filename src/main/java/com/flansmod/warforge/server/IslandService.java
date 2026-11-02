package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.CitadelBlockEntity;
import com.flansmod.warforge.common.blocks.IslandCollectorBlockEntity;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Set;

import static com.flansmod.warforge.server.ServerText.t;

/** Island collectors, force-loaded chunks and yield payout. */
public final class IslandService {
    private final FactionStorage hub;

    IslandService(FactionStorage hub) {
        this.hub = hub;
    }

    public boolean registerCollector(ServerPlayer player, DimBlockPos collectorPos) {
        Faction faction = FactionAccess.requireFaction(player);
        if (faction == null || !FactionAccess.requireRole(player, faction, Role.OFFICER)) {
            return false;
        }
        DimChunkPos chunk = collectorPos.toChunkPos();
        if (!faction.getUuid().equals(hub.claims.getClaim(chunk))) {
            player.sendSystemMessage(t("collector.own_land_only"));
            return false;
        }

        Set<DimChunkPos> island = hub.claims.collectIsland(faction.getUuid(), chunk);
        ArrayList<DimBlockPos> stale = new ArrayList<>();
        for (DimBlockPos existing : faction.getIslandCollectors()) {
            ServerLevel world = WarForgeMod.MC_SERVER.getLevel(existing.dim);
            if (world == null || !(world.getBlockEntity(existing.toRegularPos()) instanceof IslandCollectorBlockEntity)) {
                stale.add(existing);
            } else if (island.contains(existing.toChunkPos())) {
                player.sendSystemMessage(t("collector.island_has_one"));
                return false;
            }
        }
        if (!stale.isEmpty()) {
            faction.removeIslandCollectors(stale);
            WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        }

        if (!(player.level().getBlockEntity(collectorPos.toRegularPos()) instanceof IslandCollectorBlockEntity collector)) {
            return false;
        }
        collector.onServerSetFaction(faction);
        faction.addIslandCollector(collectorPos);
        WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        return true;
    }

    public void unregisterCollector(DimBlockPos collectorPos) {
        for (Faction faction : hub.registry.getAllFactions()) {
            if (faction.removeIslandCollector(collectorPos)) {
                WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
                return;
            }
        }
    }

    public boolean requestToggleForceLoad(ServerPlayer player, DimChunkPos chunk) {
        Faction faction = FactionAccess.requireFaction(player);
        if (faction == null || !FactionAccess.requireRole(player, faction, Role.OFFICER)) {
            return false;
        }
        if (!faction.getUuid().equals(hub.claims.getClaim(chunk))) {
            player.sendSystemMessage(t("forceload.own_chunks_only"));
            return false;
        }
        if (faction.unforceLoad(chunk)) {
            player.sendSystemMessage(t("forceload.removed", chunk.x, chunk.z));
        } else {
            if (!faction.canForceLoadMore()) {
                player.sendSystemMessage(t("forceload.limit", faction.getMaxForceLoadedChunks()));
                return false;
            }
            faction.forceLoad(chunk);
            player.sendSystemMessage(t("forceload.added", chunk.x, chunk.z));
        }
        WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        return true;
    }

    /** Daily yield: pending +1 per claim, then pay out loaded collectors and the citadel. */
    void awardYields(Faction faction) {
        faction.incrementPendingYields();

        ArrayList<DimBlockPos> stale = null;
        for (DimBlockPos collectorPos : faction.getIslandCollectors()) {
            ServerLevel world = WarForgeMod.MC_SERVER.getLevel(collectorPos.dim);
            if (world == null || !world.isLoaded(collectorPos.toRegularPos())) {
                continue;
            }
            BlockEntity entity = world.getBlockEntity(collectorPos.toRegularPos());
            if (entity instanceof IslandCollectorBlockEntity collector) {
                collector.processIslandYields(faction);
            } else {
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(collectorPos);
            }
        }
        if (stale != null) {
            faction.removeIslandCollectors(stale);
            WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        }

        DimBlockPos citadelPos = faction.getCitadelPos();
        ServerLevel citadelWorld = WarForgeMod.MC_SERVER.getLevel(citadelPos.dim);
        if (citadelWorld != null && citadelWorld.isLoaded(citadelPos.toRegularPos())
                && citadelWorld.getBlockEntity(citadelPos.toRegularPos()) instanceof CitadelBlockEntity citadel) {
            citadel.processFactionYields(faction);
        }
    }
}
