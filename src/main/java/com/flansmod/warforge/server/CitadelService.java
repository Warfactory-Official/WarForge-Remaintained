package com.flansmod.warforge.server;

import com.flansmod.warforge.common.Notifications;
import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.CitadelBlockEntity;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.TimeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import static com.flansmod.warforge.server.ServerText.t;

/** Relocating a faction's citadel. */
public final class CitadelService {
    private static final int PILLAR_HEIGHT = 3;
    private final FactionStorage hub;

    CitadelService(FactionStorage hub) {
        this.hub = hub;
    }

    public boolean requestMoveCitadel(ServerPlayer player, DimBlockPos pos) {
        Faction faction = FactionAccess.requireFaction(player);
        if (faction == null || !FactionAccess.requireRole(player, faction, Role.LEADER)) {
            return false;
        }
        Component denial = checkMove(player, faction, pos);
        if (denial != null) {
            player.sendSystemMessage(denial);
            return false;
        }

        DimBlockPos oldPos = faction.getCitadelPos();
        ServerLevel oldWorld = WarForgeMod.MC_SERVER.getLevel(oldPos.dim);
        ServerLevel newWorld = WarForgeMod.MC_SERVER.getLevel(pos.dim);
        if (oldWorld == null || newWorld == null
                || !(oldWorld.getBlockEntity(oldPos.toRegularPos()) instanceof CitadelBlockEntity oldCitadel)) {
            player.sendSystemMessage(t("citadel.no_current"));
            return false;
        }
        newWorld.setBlockAndUpdate(pos.toRegularPos(), Content.citadelBlock.defaultBlockState());
        if (!(newWorld.getBlockEntity(pos.toRegularPos()) instanceof CitadelBlockEntity newCitadel)) {
            player.sendSystemMessage(t("citadel.create_failed"));
            return false;
        }
        newCitadel.copyStorageFrom(oldCitadel);

        // the old citadel chunk stays claimed but loses its physical block
        for (int dy = 0; dy < PILLAR_HEIGHT; dy++) {
            BlockPos oldBlock = oldPos.toRegularPos().above(dy);
            if (!isInPillar(pos.toRegularPos(), oldBlock)) {
                oldWorld.removeBlock(oldBlock, false);
            }
        }
        boolean movingAcrossChunks = !pos.toChunkPos().equals(oldPos.toChunkPos());
        hub.claims.moveCitadel(faction, pos);
        newCitadel.onServerSetFaction(faction);
        if (movingAcrossChunks) {
            faction.setCitadelMoveTimeStamp(WarClock.citadelMoveClock());
        }
        Notifications.messageAll(t("citadel.moved", faction.getName()));
        return true;
    }

    private Component checkMove(ServerPlayer player, Faction faction, DimBlockPos pos) {
        if (hub.sieges.isDefending(faction.getUuid())) {
            return t("citadel.siege_ongoing");
        }
        if (pos == null || pos.equals(DimBlockPos.ZERO)) {
            return t("citadel.look_at_face");
        }
        if (!pos.dim.equals(player.level().dimension())) {
            return t("citadel.same_dimension");
        }
        DimBlockPos oldPos = faction.getCitadelPos();
        if (pos.equals(oldPos)) {
            return t("citadel.already_there");
        }
        DimChunkPos targetChunk = pos.toChunkPos();
        if (!faction.getUuid().equals(hub.claims.getClaim(targetChunk))) {
            return t("citadel.inside_claims");
        }
        if (!canPlaceAt(player.level(), pos, oldPos)) {
            return t("citadel.no_space");
        }
        if (pos.getY() < WarForgeConfig.CITADEL_MIN_Y) {
            return t("citadel.min_y", WarForgeConfig.CITADEL_MIN_Y);
        }
        boolean acrossChunks = !targetChunk.equals(oldPos.toChunkPos());
        long readyAt = faction.getCitadelMoveTimeStamp() + TimeHelper.getCitadelMoveCooldownMs();
        if (acrossChunks && faction.getCitadelMoveTimeStamp() > 0L && WarClock.citadelMoveClock() < readyAt) {
            return t("citadel.cooldown", TimeHelper.formatTime(readyAt - WarClock.citadelMoveClock()));
        }
        if (!acrossChunks || faction.getSpecificPosForClaim(targetChunk) != null) {
            return null;
        }
        return t("citadel.no_claim_pos");
    }

    private static boolean canPlaceAt(Level world, DimBlockPos pos, DimBlockPos oldCitadelPos) {
        BlockPos below = pos.below();
        if (!world.getBlockState(below).isFaceSturdy(world, below, Direction.UP)) {
            return false;
        }
        for (int dy = 0; dy < PILLAR_HEIGHT; dy++) {
            if (!isSpaceFree(world, pos.above(dy), oldCitadelPos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInPillar(BlockPos base, BlockPos candidate) {
        return candidate.getX() == base.getX() && candidate.getZ() == base.getZ()
                && candidate.getY() >= base.getY() && candidate.getY() < base.getY() + PILLAR_HEIGHT;
    }

    private static boolean isSpaceFree(Level world, BlockPos pos, DimBlockPos oldCitadelPos) {
        if (isInPillar(oldCitadelPos.toRegularPos(), pos)) {
            return true;
        }
        return world.isEmptyBlock(pos) || world.getBlockState(pos).canBeReplaced();
    }
}
