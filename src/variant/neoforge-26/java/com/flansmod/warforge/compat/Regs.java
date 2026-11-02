package com.flansmod.warforge.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public final class Regs {
    private Regs() {}

    public static <T> T get(Registry<T> registry, Identifier id) {
        return registry.getValue(id);
    }

    public static <T> boolean hasTag(Registry<T> registry, TagKey<T> tag) {
        return registry.get(tag).isPresent();
    }

    public static Ingredient ingredient(TagKey<Item> tag) {
        return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(tag));
    }

    public static List<ItemStack> stacks(TagKey<Item> tag) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            stacks.add(new ItemStack(item));
        }
        return stacks;
    }
}
