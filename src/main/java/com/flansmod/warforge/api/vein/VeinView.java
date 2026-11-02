package com.flansmod.warforge.api.vein;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/**
 * Read-only vein definition. {@code dimWeights}: 0..1 share of chunks in the dimension.
 */
public record VeinView(short id, String key, int wealth, Map<ResourceKey<Level>, Double> dimWeights,
                       List<Component> components) {
    /** {@code yield}: base yield per roll before dimension and quality multipliers. */
    public record Component(String item, float yield) {}
}
