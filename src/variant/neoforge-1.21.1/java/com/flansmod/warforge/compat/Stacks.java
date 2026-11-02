package com.flansmod.warforge.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class Stacks {
    private Stacks() {}

    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
        return (CompoundTag) stack.saveOptional(registries);
    }

    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag) {
        return ItemStack.parseOptional(registries, tag);
    }

    public static boolean sameItemSameData(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    public static boolean isFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    public static String dataKey(ItemStack stack) {
        return String.valueOf(stack.getComponentsPatch());
    }
}
