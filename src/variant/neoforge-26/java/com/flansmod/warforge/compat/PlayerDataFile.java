package com.flansmod.warforge.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PlayerDataFile {
    private PlayerDataFile() {}

    public static List<ItemStack> takeInventory(CompoundTag playerData, HolderLookup.Provider registries) {
        ListTag inventoryList = Nbt.getList(playerData, "Inventory", Tag.TAG_COMPOUND).copy();
        playerData.put("Inventory", new ListTag());
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inventoryList.size(); i++) {
            stacks.add(Stacks.parse(registries, Nbt.getCompound(inventoryList, i)));
        }
        // 1.21.5+: armour and offhand live in "equipment" (slot name -> item)
        CompoundTag equipment = Nbt.getCompound(playerData, "equipment");
        for (String slot : equipment.keySet()) {
            stacks.add(Stacks.parse(registries, Nbt.getCompound(equipment, slot)));
        }
        playerData.remove("equipment");
        return stacks;
    }
}
