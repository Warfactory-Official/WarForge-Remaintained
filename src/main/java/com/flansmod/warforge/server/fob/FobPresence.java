package com.flansmod.warforge.server.fob;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Chunks;
import com.flansmod.warforge.server.Faction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class FobPresence {
    private enum Relation {OWNER, ALLY, TRUCE, ENEMY}

    private FobPresence() {
    }

    /** No faction counts as enemy; a post-break truce counts as neither enemy nor ally. */
    private static Relation classify(Faction owner, Faction playerFaction) {
        if (playerFaction == null) {
            return Relation.ENEMY;
        }
        if (playerFaction == owner) {
            return Relation.OWNER;
        }
        if (owner.isAllyOf(playerFaction.getUuid())) {
            return Relation.ALLY;
        }
        return owner.isInTruceWith(playerFaction.getUuid()) ? Relation.TRUCE : Relation.ENEMY;
    }

    public static boolean enemyHeld(Fob fob) {
        return anyAbove(fob, true);
    }

    public static boolean ownerOrAllyAbove(Fob fob) {
        return anyAbove(fob, false);
    }

    /** @param enemies true: any enemy above; false: any owner or ally above */
    private static boolean anyAbove(Fob fob, boolean enemies) {
        if (fob == null || WarForgeMod.MC_SERVER == null) {
            return false;
        }
        Faction owner = WarForgeMod.FACTIONS.registry.getFaction(fob.ownerFaction);
        if (owner == null) {
            return false;
        }
        DimChunkPos fobChunk = fob.pos.toChunkPos();
        for (ServerPlayer player : WarForgeMod.MC_SERVER.getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(fob.pos.dim) || player.getY() <= fob.pos.getY()) {
                continue;
            }
            ChunkPos playerChunk = Chunks.of(player.blockPosition());
            if (Chunks.x(playerChunk) != fobChunk.x || Chunks.z(playerChunk) != fobChunk.z) {
                continue;
            }
            Relation relation = classify(owner, WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID()));
            if (enemies ? relation == Relation.ENEMY : (relation == Relation.OWNER || relation == Relation.ALLY)) {
                return true;
            }
        }
        return false;
    }
}
