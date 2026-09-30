package com.flansmod.warforge.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Item stack save/load and comparison. The item stack format and API differ between versions. */
public final class Stacks {
    private Stacks() {}

    /** Saves a stack (an empty stack gives an empty compound). */
    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
        return (CompoundTag) stack.saveOptional(registries);
    }

    /** Loads a stack saved by {@link #save}. Returns ItemStack.EMPTY for an empty or invalid tag. */
    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag) {
        return ItemStack.parseOptional(registries, tag);
    }

    /** Same item and same data (components / NBT). The count is not compared. */
    public static boolean sameItemSameData(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    public static boolean isFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    /** A string that is equal for stacks with equal data (for cache keys). */
    public static String dataKey(ItemStack stack) {
        return String.valueOf(stack.getComponentsPatch());
    }
}
