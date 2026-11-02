package com.flansmod.warforge.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class Stacks {
    private Stacks() {}

    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
        return stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag());
    }

    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(tag);
    }

    public static boolean sameItemSameData(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    public static boolean isFood(ItemStack stack) {
        return stack.isEdible();
    }

    public static String dataKey(ItemStack stack) {
        return String.valueOf(stack.getTag());
    }
}
