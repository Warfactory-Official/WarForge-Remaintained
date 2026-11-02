package com.flansmod.warforge.compat;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

public final class Regs {
    private Regs() {}

    public static <T> T get(Registry<T> registry, ResourceLocation id) {
        return registry.get(id);
    }

    public static <T> boolean hasTag(Registry<T> registry, TagKey<T> tag) {
        return registry.getTag(tag).isPresent();
    }

    public static Ingredient ingredient(TagKey<Item> tag) {
        return Ingredient.of(tag);
    }

    public static List<ItemStack> stacks(TagKey<Item> tag) {
        return Arrays.asList(Ingredient.of(tag).getItems());
    }
}
