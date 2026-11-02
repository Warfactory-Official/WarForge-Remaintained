package com.flansmod.warforge.compat;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Main-inventory list is private from 1.21.5. */
public final class Inventories {
    private Inventories() {}

    public static NonNullList<ItemStack> main(Inventory inventory) {
        return inventory.getNonEquipmentItems();
    }
}
