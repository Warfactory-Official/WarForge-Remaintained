package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.compat.Nbt;
import com.flansmod.warforge.compat.Stacks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

/** Insurance stashes unlocked by a faction's defeat, redeemable by the former leader. */
public final class InsuranceVaults {
    private final HashMap<UUID, ArrayList<ItemStack>> redeemable = new HashMap<>();

    /** Moves the faction's insurance contents into its leader's redeemable stash. */
    void unlock(Faction faction) {
        if (!faction.hasInsuranceContents()) {
            return;
        }
        UUID leaderId = faction.getLeaderId();
        if (leaderId.equals(FactionIds.NONE)) {
            WarForgeMod.LOGGER.warn("Could not unlock insurance stash for faction {}: no leader", faction.getName());
            return;
        }
        redeemable.computeIfAbsent(leaderId, id -> new ArrayList<>()).addAll(faction.drainInsuranceContents());
        ServerPlayer leader = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(leaderId);
        if (leader != null) {
            leader.sendSystemMessage(t("insurance.unlocked"));
        }
    }

    public boolean redeem(ServerPlayer player) {
        ArrayList<ItemStack> items = redeemable.get(player.getUUID());
        if (items == null || items.isEmpty()) {
            player.sendSystemMessage(t("insurance.none"));
            return false;
        }
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack redeemStack = stack.copy();
            if (!player.getInventory().add(redeemStack) && !redeemStack.isEmpty()) {
                player.drop(redeemStack, false);
            }
        }
        redeemable.remove(player.getUUID());
        player.sendSystemMessage(t("insurance.redeemed"));
        return true;
    }

    /** Joining a new faction voids an unredeemed stash. */
    void invalidate(Player player) {
        ArrayList<ItemStack> removed = redeemable.remove(player.getUUID());
        if (removed != null && !removed.isEmpty()) {
            player.sendSystemMessage(t("insurance.voided"));
        }
    }

    void readFromNBT(CompoundTag tags) {
        redeemable.clear();
        for (Tag base : Nbt.getList(tags, "redeemableInsuranceVaults", Tag.TAG_COMPOUND)) {
            CompoundTag payoutTag = (CompoundTag) base;
            ArrayList<ItemStack> items = new ArrayList<>();
            for (Tag itemBase : Nbt.getList(payoutTag, "items", Tag.TAG_COMPOUND)) {
                items.add(Stacks.parse(WarForgeMod.MC_SERVER.registryAccess(), (CompoundTag) itemBase));
            }
            redeemable.put(Nbt.getUUID(payoutTag, "owner"), items);
        }
    }

    void writeToNBT(CompoundTag tags) {
        ListTag payoutList = new ListTag();
        for (Map.Entry<UUID, ArrayList<ItemStack>> entry : redeemable.entrySet()) {
            CompoundTag payoutTag = new CompoundTag();
            Nbt.putUUID(payoutTag, "owner", entry.getKey());
            ListTag itemsList = new ListTag();
            for (ItemStack stack : entry.getValue()) {
                if (stack != null && !stack.isEmpty()) {
                    itemsList.add(Stacks.save(stack, WarForgeMod.MC_SERVER.registryAccess()));
                }
            }
            payoutTag.put("items", itemsList);
            payoutList.add(payoutTag);
        }
        tags.put("redeemableInsuranceVaults", payoutList);
    }
}
