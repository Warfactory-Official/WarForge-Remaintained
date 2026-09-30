package com.flansmod.warforge.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/** Item stack save/load and comparison. The item stack format and API differ between versions. */
public final class Stacks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private Stacks() {}

    /** Saves a stack (an empty stack gives an empty compound). */
    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return new CompoundTag();
        return (CompoundTag) ItemStack.OPTIONAL_CODEC
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow();
    }

    /** Loads a stack saved by {@link #save}. Returns ItemStack.EMPTY for an empty or invalid tag. */
    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.isEmpty()) return ItemStack.EMPTY;
        return ItemStack.OPTIONAL_CODEC
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> LOGGER.error("Tried to load invalid item: '{}'", error))
                .orElse(ItemStack.EMPTY);
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
