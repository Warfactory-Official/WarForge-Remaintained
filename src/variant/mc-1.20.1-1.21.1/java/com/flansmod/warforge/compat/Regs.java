package com.flansmod.warforge.compat;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

/** Registry and tag lookups. Registry.get and the tag/ingredient API differ between versions. */
public final class Regs {
    private Regs() {}

    /** The value registered at id (the registry's default value, or null, if there is none). */
    public static <T> T get(Registry<T> registry, ResourceLocation id) {
        return registry.get(id);
    }

    /** True if the tag exists (is bound) in the registry. */
    public static <T> boolean hasTag(Registry<T> registry, TagKey<T> tag) {
        return registry.getTag(tag).isPresent();
    }

    public static Ingredient ingredient(TagKey<Item> tag) {
        return Ingredient.of(tag);
    }

    /** One stack of each item in the tag, in tag order. */
    public static List<ItemStack> stacks(TagKey<Item> tag) {
        return Arrays.asList(Ingredient.of(tag).getItems());
    }
}
