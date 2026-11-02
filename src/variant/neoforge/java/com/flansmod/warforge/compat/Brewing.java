package com.flansmod.warforge.compat;

import com.flansmod.warforge.platform.Platform;
import com.flansmod.warforge.platform.RegHandle;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

import java.util.function.Consumer;

public final class Brewing {
    private Brewing() {}

    public interface Mixer {
        void addMix(ResourceKey<Potion> base, Item ingredient, RegHandle<Potion> result);
    }

    public static void register(Consumer<Mixer> recipes) {
        Platform.gameBus().addListener((RegisterBrewingRecipesEvent event) -> recipes.accept((base, ingredient, result) -> {
            Holder<Potion> baseHolder = event.getRegistryAccess().lookupOrThrow(Registries.POTION).getOrThrow(base);
            event.getBuilder().addMix(baseHolder, ingredient, result.holder());
        }));
    }
}
