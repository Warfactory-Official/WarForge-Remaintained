package com.flansmod.warforge.server;

import com.flansmod.warforge.api.SiegeDefence;
import com.flansmod.warforge.api.WarforgeAPI;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.blocks.IClaim;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The siege difficulty formula of a defending chunk; the single source for sieges and the API. */
public final class SiegeDifficulty {
    public static final int MEMBER_BONUS_CAP = 5;

    private SiegeDifficulty() {}

    /** Base claim defence: the claim block's strength, else the claim type's. */
    public static int baseDefence(MinecraftServer server, Faction faction, DimChunkPos chunk) {
        DimBlockPos pos = faction.getSpecificPosForClaim(chunk);
        if (pos != null) {
            ServerLevel level = server.getLevel(pos.dim);
            if (level != null && level.getBlockEntity(pos.toRegularPos()) instanceof IClaim claim) {
                return claim.getDefenceStrength();
            }
        }
        return faction.getClaimType(chunk).defenceStrength();
    }

    public static int memberBonus(Faction faction) {
        return Math.min(MEMBER_BONUS_CAP, faction.getMemberCount() * WarForgeConfig.SIEGE_DIFF_PER_MEMBER);
    }

    /** Summed support of cardinal neighbours owned by the same faction. */
    public static int adjacentSupport(MinecraftServer server, Faction faction, DimChunkPos chunk) {
        int support = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            DimChunkPos neighbor = chunk.offset(dir, 1);
            if (!faction.getUuid().equals(com.flansmod.warforge.common.WarForgeMod.FACTIONS.claims.getClaim(neighbor))) {
                continue;
            }
            DimBlockPos pos = faction.getSpecificPosForClaim(neighbor);
            if (pos == null) {
                continue;
            }
            ServerLevel level = server.getLevel(pos.dim);
            BlockEntity be = level == null ? null : level.getBlockEntity(pos.toRegularPos());
            support += be instanceof IClaim claim ? claim.getSupportStrength() : faction.getClaimType(neighbor).supportStrength();
        }
        return support;
    }

    public static SiegeDefence breakdown(MinecraftServer server, Faction faction, DimChunkPos chunk) {
        int base = baseDefence(server, faction, chunk);
        int member = memberBonus(faction);
        int support = adjacentSupport(server, faction, chunk);
        int reinforcement = WarforgeAPI.getReinforcementBonus(faction.getUuid(), chunk.dim, new net.minecraft.world.level.ChunkPos(chunk.x, chunk.z));
        return new SiegeDefence(base, member, support, reinforcement, base + member - support + reinforcement);
    }
}
