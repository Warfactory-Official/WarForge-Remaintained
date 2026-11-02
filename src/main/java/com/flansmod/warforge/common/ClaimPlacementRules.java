package com.flansmod.warforge.common;

import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.item.ItemMatcher;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Inventories;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.flansmod.warforge.common.WarForgeMod.FACTIONS;
import static com.flansmod.warforge.common.WarForgeMod.FOBS;

/** What a player holding a claim-type item may place where; the first failing rule is returned. */
public final class ClaimPlacementRules {

    private ClaimPlacementRules() {}

    /** @return the denial message, or null when placement is allowed */
    @Nullable
    public static Component check(Player player, Level level, ItemStack stack, BlockPos clicked, @Nullable Direction face) {
        Item item = stack.getItem();
        BlockPos placementPos = clicked.relative(face != null ? face : Direction.UP);
        DimChunkPos chunk = new DimBlockPos(level.dimension(), placementPos).toChunkPos();
        Faction faction = FACTIONS.registry.getFactionOfPlayer(player.getUUID());

        if (item.equals(Content.FOB_BLOCK_ITEM.get())) {
            return checkFob(player, faction, chunk);
        }
        if (!Content.isClaimItem(item)) {
            return null;
        }

        Block block = ((BlockItem) item).getBlock();
        if (block == Content.citadelBlock) {
            return FACTIONS.claimPolicy.checkFoundFaction(player, chunk);
        }
        if (block == Content.basicClaimBlock || block == Content.reinforcedClaimBlock) {
            if (faction == null) {
                return Component.translatable("warforge.common.place.no_faction");
            }
            Component denial = FACTIONS.claimPolicy.checkClaim(player, faction, chunk, null);
            return denial != null ? denial : missingExtraCost(player, faction);
        }
        Component denial = FACTIONS.claimPolicy.checkChunkFree(chunk);
        return denial != null ? denial : checkSiegeCamp(player, faction, level, clicked);
    }

    @Nullable
    private static Component checkFob(Player player, @Nullable Faction faction, DimChunkPos chunk) {
        if (faction == null) {
            return Component.translatable("warforge.common.place.no_faction");
        }
        if (!faction.isPlayerRoleInFaction(player.getUUID(), Role.OFFICER)) {
            return Component.translatable("warforge.common.place.not_officer");
        }
        Component fobDenial = FOBS.checkPlacement(faction, chunk);
        if (fobDenial != null) {
            return fobDenial;
        }
        return null;
    }

    @Nullable
    private static Component checkSiegeCamp(Player player, @Nullable Faction faction, Level level, BlockPos placementPos) {
        if (faction == null) {
            return Component.translatable("warforge.common.place.no_faction");
        }
        if (!faction.isPlayerRoleInFaction(player.getUUID(), Role.OFFICER)) {
            return Component.translatable("warforge.common.place.not_officer");
        }
        if (WarForgeConfig.MAX_SIEGES > 0 && FACTIONS.sieges.countAttacking(faction.getUuid()) >= WarForgeConfig.MAX_SIEGES) {
            return Component.translatable("warforge.info.too_many_siege_blocks");
        }
        if (FACTIONS.sieges.getAdjacentClaims(faction.getUuid(), new DimBlockPos(level.dimension(), placementPos)).isEmpty()) {
            return Component.translatable("warforge.common.place.no_adjacent_claims", WarForgeConfig.VERTICAL_SIEGE_DIST);
        }
        return null;
    }

    private static Map<ItemMatcher, Integer> extraCost(Player player, Faction faction) {
        if (!WarForgeConfig.ENABLE_CITADEL_UPGRADES || player.getAbilities().instabuild) {
            return Map.of();
        }
        Map<ItemMatcher, Integer> cost = WarForgeMod.UPGRADE_HANDLER.getExtraClaimCostForLevel(faction.getCitadelLevel());
        return cost == null ? Map.of() : cost;
    }

    @Nullable
    private static Component missingExtraCost(Player player, Faction faction) {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<ItemMatcher, Integer> entry : extraCost(player, faction).entrySet()) {
            int have = countMatchingItems(player, entry.getKey());
            if (have < entry.getValue()) {
                missing.add(entry.getValue() + " " + describeCost(entry.getKey()) + " (have " + have + ")");
            }
        }
        return missing.isEmpty() ? null : Component.translatable("warforge.common.place.extra_cost",
                faction.getCitadelLevel(), String.join(", ", missing));
    }

    /** Called once the claim is registered, so a refused placement never costs items. */
    public static void chargeExtraCost(Player player, Faction faction) {
        for (Map.Entry<ItemMatcher, Integer> entry : extraCost(player, faction).entrySet()) {
            consumeMatchingItems(player, entry.getKey(), entry.getValue());
        }
    }

    private static String describeCost(ItemMatcher target) {
        ItemStack sample = target.toStack(1);
        return sample != null && !sample.isEmpty() ? sample.getHoverName().getString() : target.id();
    }

    private static int countMatchingItems(Player player, ItemMatcher target) {
        int total = 0;
        for (ItemStack stack : Inventories.main(player.getInventory())) {
            if (!stack.isEmpty() && target.matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeMatchingItems(Player player, ItemMatcher target, int amount) {
        int remaining = amount;
        var inventory = Inventories.main(player.getInventory());
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty() || !target.matches(stack)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        player.inventoryMenu.broadcastChanges();
    }
}
