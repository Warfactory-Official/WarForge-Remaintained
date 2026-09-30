package com.flansmod.warforge.compat;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Player inventory access. The main-inventory list became private in 1.21.5. */
public final class Inventories {
    private Inventories() {}

    /** The 36 main inventory slots (hotbar + storage), as a live list. */
    public static NonNullList<ItemStack> main(Inventory inventory) {
        return inventory.items;
    }
}
