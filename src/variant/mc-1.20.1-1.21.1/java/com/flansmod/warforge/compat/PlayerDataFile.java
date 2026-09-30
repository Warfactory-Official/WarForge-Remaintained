package com.flansmod.warforge.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Offline player data (playerdata/&lt;uuid&gt;.dat). Where the items are saved differs between versions. */
public final class PlayerDataFile {
    private PlayerDataFile() {}

    /** Removes every item (inventory, armour, offhand) from the player data and returns them. */
    public static List<ItemStack> takeInventory(CompoundTag playerData, HolderLookup.Provider registries) {
        ListTag inventoryList = Nbt.getList(playerData, "Inventory", Tag.TAG_COMPOUND).copy();
        playerData.put("Inventory", new ListTag());
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inventoryList.size(); i++) {
            stacks.add(Stacks.parse(registries, Nbt.getCompound(inventoryList, i)));
        }
        return stacks;
    }
}
