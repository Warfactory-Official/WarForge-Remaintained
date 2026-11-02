package com.flansmod.warforge.server;

import com.flansmod.warforge.common.Notifications;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.IClaim;
import com.flansmod.warforge.common.blocks.BasicClaimBlockEntity;
import com.flansmod.warforge.common.blocks.ReinforcedClaimBlockEntity;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Inventories;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

public final class ClaimService {
    private static final int CLAIM_BLOCK_HEIGHT = 3;
    private final FactionStorage hub;

    ClaimService(FactionStorage hub) {
        this.hub = hub;
    }

    /** Every position holding a claim-like block entity: claims, island collectors and attacking siege camps. */
    Set<DimBlockPos> collectFactionPositions(Faction faction) {
        Set<DimBlockPos> positions = new HashSet<>(faction.getClaimPositions());
        positions.addAll(faction.getIslandCollectors());
        for (Siege siege : hub.sieges.all()) {
            if (faction.getUuid().equals(siege.attackingFaction)) {
                for (DimBlockPos camp : siege.attackingCamps) {
                    if (camp != null) positions.add(camp);
                }
            }
        }
        return positions;
    }

    public void onNonCitadelClaimPlaced(IClaim claim, LivingEntity placer) {
        Faction faction = hub.registry.getFactionOfPlayer(placer.getUUID());
        if (faction == null) {
            WarForgeMod.LOGGER.error("Claim placed at {} by {} who has no faction", claim.getClaimPos(), placer.getName().getString());
            return;
        }
        onNonCitadelClaimPlaced(claim, faction);
        hub.notifier.claimChanged(faction, "claim_added_" + claim.getClaimPos(), t("toast.chunk_claimed.title"),
                t("toast.chunk_claimed.body", placer.getName().getString(), claim.getClaimPos().toFancyString()),
                faction.getColour(), placer.getUUID());
    }

    public void onNonCitadelClaimPlaced(IClaim claim, Faction faction) {
        BlockEntity blockEntity = claim.getAsBlockEntity();
        boolean dataOnlyClaim = blockEntity instanceof BasicClaimBlockEntity || blockEntity instanceof ReinforcedClaimBlockEntity;
        DimBlockPos claimPos = claim.getClaimPos();
        hub.claims.claim(faction, claimPos, ClaimType.fromClaim(claim));
        faction.messageAll(t("claim.claimed_around", claimPos.toChunkPos().x, claimPos.toChunkPos().z, claimPos.toFancyString()));

        if (!dataOnlyClaim) {
            claim.onServerSetFaction(faction);
            return;
        }
        // basic and reinforced claim blocks are consumed on placement; the claim lives in data
        Level world = blockEntity.getLevel();
        for (int dy = 0; dy < CLAIM_BLOCK_HEIGHT; dy++) {
            world.removeBlock(claimPos.toRegularPos().above(dy), false);
        }
    }

    public boolean requestClaimChunk(ServerPlayer player, DimChunkPos chunk) {
        Faction faction = FactionAccess.requireFaction(player);
        if (faction == null) {
            return false;
        }
        Component denial = hub.claimPolicy.checkClaim(player, faction, chunk, null);
        if (denial != null) {
            player.sendSystemMessage(denial);
            return false;
        }
        ClaimedBlockSelection selection = findFirstClaimBlock(player.getInventory());
        if (selection == null) {
            player.sendSystemMessage(t("claim.need_block"));
            return false;
        }
        hub.claims.claimData(faction, chunk, player.blockPosition().getY(), selection.claimType());
        selection.consume(player.getInventory());
        faction.messageAll(t("claim.claimed", chunk.x, chunk.z));
        hub.notifier.claimChanged(faction, "claim_added_" + chunk, t("toast.chunk_claimed.title"),
                t("toast.chunk_claimed.body", player.getName().getString(), "[" + chunk.x + ", " + chunk.z + "]"),
                faction.getColour(), player.getUUID());
        return true;
    }

    public void requestZoneClaim(Player op, DimChunkPos chunk, UUID zoneId) {
        if (!WarForgeMod.isOp(op)) {
            op.sendSystemMessage(t("zone.op_required"));
            return;
        }
        Faction zone = hub.registry.getFaction(zoneId);
        if (zone == null || !FactionIds.isNeutralZone(zoneId)) {
            op.sendSystemMessage(t("zone.unknown"));
            return;
        }
        if (hub.claims.isClaimed(chunk)) {
            op.sendSystemMessage(t("zone.already_claimed"));
            return;
        }
        hub.claims.claimData(zone, chunk, op.blockPosition().getY(),
                zoneId.equals(FactionIds.SAFE_ZONE) ? ClaimType.ADMIN : ClaimType.WARZONE);
        op.sendSystemMessage(t("zone.claimed", chunk.x, chunk.z, zone.getName()));
    }

    public void requestZoneUnclaim(Player op, DimChunkPos chunk) {
        if (!WarForgeMod.isOp(op)) {
            op.sendSystemMessage(t("zone.op_required"));
            return;
        }
        UUID owner = hub.claims.getClaim(chunk);
        Faction zone = FactionIds.isNeutralZone(owner) ? hub.registry.getFaction(owner) : null;
        DimBlockPos claimPos = zone == null ? null : zone.getSpecificPosForClaim(chunk);
        if (claimPos == null) {
            op.sendSystemMessage(t("zone.none_here"));
            return;
        }
        hub.claims.unclaim(zone, claimPos);
        op.sendSystemMessage(t("zone.removed", zone.getName(), chunk.x, chunk.z));
    }

    public boolean requestRemoveClaimByChunk(ServerPlayer player, DimChunkPos chunk) {
        Faction faction = hub.registry.getFaction(hub.claims.getClaim(chunk));
        DimBlockPos claimPos = faction == null ? null : faction.getSpecificPosForClaim(chunk);
        if (claimPos == null) {
            player.sendSystemMessage(t("claim.not_found"));
            return false;
        }
        return requestRemoveClaim(player, claimPos);
    }

    public boolean requestRemoveClaim(ServerPlayer player, DimBlockPos pos) {
        DimChunkPos chunk = pos.toChunkPos();
        Faction faction = hub.registry.getFaction(hub.claims.getClaim(chunk));
        if (faction == null) {
            player.sendSystemMessage(t("claim.not_found"));
            return false;
        }
        DimBlockPos claimPos = faction.getSpecificPosForClaim(chunk);
        if (claimPos != null) {
            pos = claimPos;
        }
        if (!FactionAccess.requireRole(player, faction, Role.OFFICER)) {
            return false;
        }
        Component blocked = removalBlockedReason(faction, pos);
        if (blocked != null) {
            player.sendSystemMessage(blocked);
            return false;
        }

        ClaimType claimType = faction.getClaimType(chunk);
        ServerLevel claimWorld = WarForgeMod.MC_SERVER.getLevel(pos.dim);
        boolean dataOnlyClaim = claimWorld != null && !Content.isClaimAnchor(claimWorld.getBlockState(pos.toRegularPos()).getBlock());

        onClaimLost(faction, pos, false, false);
        if (dataOnlyClaim) {
            refund(player, claimType);
        }
        faction.messageAll(t("claim.unclaimed", player.getName().getString(), pos.toFancyString()));
        hub.notifier.claimChanged(faction, "claim_removed_" + pos, t("toast.chunk_unclaimed.title"),
                t("toast.chunk_unclaimed.body", player.getName().getString(), pos.toFancyString()), FactionNotifier.WARNING, player.getUUID());
        return true;
    }

    public boolean requestRemoveClaimServer(DimBlockPos pos) {
        Faction faction = hub.registry.getFaction(hub.claims.getClaim(pos));
        if (faction == null) {
            return false;
        }
        DimBlockPos claimPos = faction.getSpecificPosForClaim(pos.toChunkPos());
        if (claimPos != null) {
            pos = claimPos;
        }
        if (removalBlockedReason(faction, pos) != null) {
            return false;
        }
        onClaimLost(faction, pos, false, false);
        hub.notifier.claimChanged(faction, "claim_removed_" + pos, t("toast.chunk_unclaimed.title"),
                t("toast.claim_removed.body", pos.toFancyString()), FactionNotifier.WARNING, null);
        return true;
    }

    private Component removalBlockedReason(Faction faction, DimBlockPos pos) {
        if (pos.equals(faction.getCitadelPos())) {
            return t("claim.cant_remove_citadel");
        }
        DimChunkPos chunk = pos.toChunkPos();
        for (Map.Entry<DimChunkPos, Siege> siege : hub.sieges.entries().entrySet()) {
            if (siege.getKey().equals(chunk)) {
                return t("claim.under_siege");
            }
            if (siege.getValue().attackingCamps.contains(pos)) {
                return t("claim.camp_in_siege");
            }
        }
        return null;
    }

    public void onClaimLost(Faction faction, DimBlockPos pos, boolean captureAttempted, boolean notifyLoss) {
        ServerLevel world = WarForgeMod.MC_SERVER.getLevel(pos.dim);
        if (world != null) {
            BlockState claimBlock = world.getBlockState(pos.toRegularPos());
            if (Content.isClaimAnchor(claimBlock.getBlock())) {
                world.removeBlock(pos.toRegularPos(), false);
                if (!captureAttempted || !WarForgeConfig.SIEGE_CAPTURE) {
                    world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d,
                            new ItemStack(claimBlock.getBlock().asItem())));
                }
            }
        }
        if (pos.equals(faction.getCitadelPos())) {
            hub.lifecycle.defeat(faction);
            Notifications.messageAll(t("claim.citadel_destroyed", faction.getName(), faction.getName()));
            return;
        }
        if (notifyLoss) {
            faction.messageAll(t("claim.lost", pos.toFancyString()));
            hub.notifier.claimChanged(faction, "claim_lost_" + pos, t("toast.claim_lost.title"),
                    t("toast.claim_lost.body", pos.toFancyString()), FactionNotifier.DANGER, null);
        }
        hub.claims.unclaim(faction, pos);
    }

    private record ClaimedBlockSelection(int slot, ClaimType claimType) {
        private void consume(Inventory inventory) {
            ItemStack stack = Inventories.main(inventory).get(slot);
            stack.shrink(1);
            if (stack.isEmpty()) {
                Inventories.main(inventory).set(slot, ItemStack.EMPTY);
            }
        }
    }

    private static ClaimedBlockSelection findFirstClaimBlock(Inventory inventory) {
        for (int slot = 0; slot < Inventories.main(inventory).size(); slot++) {
            ItemStack stack = Inventories.main(inventory).get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == Content.basicClaimBlockItem) {
                return new ClaimedBlockSelection(slot, ClaimType.BASIC);
            }
            if (stack.getItem() == Content.reinforcedClaimBlockItem) {
                return new ClaimedBlockSelection(slot, ClaimType.REINFORCED);
            }
        }
        return null;
    }

    private static void refund(ServerPlayer player, ClaimType claimType) {
        ItemStack refund = switch (claimType) {
            case BASIC -> new ItemStack(Content.basicClaimBlockItem);
            case REINFORCED -> new ItemStack(Content.reinforcedClaimBlockItem);
            default -> ItemStack.EMPTY;
        };
        if (refund.isEmpty()) {
            return;
        }
        if (player.getInventory().add(refund)) {
            player.inventoryMenu.broadcastChanges();
        } else {
            player.drop(refund, false);
        }
    }
}
