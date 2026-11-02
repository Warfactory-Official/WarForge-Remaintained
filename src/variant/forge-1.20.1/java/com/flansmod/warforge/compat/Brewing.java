package com.flansmod.warforge.compat;

import com.flansmod.warforge.platform.RegHandle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Consumer;

public final class Brewing {
    private Brewing() {}

    public interface Mixer {
        void addMix(ResourceKey<Potion> base, Item ingredient, RegHandle<Potion> result);
    }

    private static final Item[] BOTTLES = {Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION};

    public static void register(Consumer<Mixer> recipes) {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                recipes.accept((base, ingredient, result) -> {
                    Potion basePotion = BuiltInRegistries.POTION.get(base.location());
                    for (Item bottle : BOTTLES) {
                        ItemStack input = PotionUtils.setPotion(new ItemStack(bottle), basePotion);
                        ItemStack output = PotionUtils.setPotion(new ItemStack(bottle), result.get());
                        BrewingRecipeRegistry.addRecipe(StrictNBTIngredient.of(input), Ingredient.of(ingredient), output);
                    }
                })));
    }
}
