package com.flansmod.warforge.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class Stacks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private Stacks() {}

    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return new CompoundTag();
        return (CompoundTag) ItemStack.OPTIONAL_CODEC
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow();
    }

    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.isEmpty()) return ItemStack.EMPTY;
        return ItemStack.OPTIONAL_CODEC
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> LOGGER.error("Tried to load invalid item: '{}'", error))
                .orElse(ItemStack.EMPTY);
    }

    /** Count not compared. */
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
