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

/** Brewing-stand recipes. NeoForge collects them in an event; Forge 1.20.1 has a static registry. */
public final class Brewing {
    private Brewing() {}

    public interface Mixer {
        /** A bottle (potion, splash or lingering) of base + ingredient gives the same bottle of result. */
        void addMix(ResourceKey<Potion> base, Item ingredient, RegHandle<Potion> result);
    }

    /** Runs the callback when brewing recipes are collected. */
    public static void register(Consumer<Mixer> recipes) {
        Platform.gameBus().addListener((RegisterBrewingRecipesEvent event) -> recipes.accept((base, ingredient, result) -> {
            @SuppressWarnings("unchecked")
            Holder<Potion> resultHolder = (Holder<Potion>) result.holder();
            Holder<Potion> baseHolder = event.getRegistryAccess().lookupOrThrow(Registries.POTION).getOrThrow(base);
            event.getBuilder().addMix(baseHolder, ingredient, resultHolder);
        }));
    }
}
