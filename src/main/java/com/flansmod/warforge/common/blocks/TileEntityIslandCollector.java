package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;

import java.util.Set;

public class TileEntityIslandCollector extends TileEntityYieldCollector {
    @Override
    public int getDefenceStrength() {
        return 0;
    }

    @Override
    public int getSupportStrength() {
        return 0;
    }

    @Override
    public int getAttackStrength() {
        return 0;
    }

    @Override
    public boolean canBeSieged() {
        return false;
    }

    @Override
    protected float getYieldMultiplier() {
        return 1.0f;
    }

    @Override
    public String getClaimDisplayName() {
        return factionName.isEmpty() ? "Faction Yield Storage" : factionName + " Yield Storage";
    }

    @Override
    public String getName() {
        return getClaimDisplayName();
    }

    @Override
    public boolean hasCustomName() {
        return true;
    }

    public void processIslandYields(Faction faction) {
        if (world == null || world.isRemote || faction == null) {
            return;
        }

        DimChunkPos collectorChunk = new DimChunkPos(world.provider.getDimension(), pos);
        Set<DimChunkPos> island = WarForgeMod.FACTIONS.collectFactionIsland(faction.uuid, collectorChunk);
        if (island.isEmpty()) {
            return;
        }

        for (DimBlockPos claimPos : faction.claims.keySet()) {
            if (island.contains(claimPos.toChunkPos())) {
                processYieldForClaim(faction.claims, claimPos);
            }
        }
    }

    @Override
    public void onLoad() {
        if (world == null || world.isRemote) {
            return;
        }

        Faction faction = WarForgeMod.FACTIONS.getFaction(factionUUID);
        if (faction != null) {
            processIslandYields(faction);
        }
    }
}
