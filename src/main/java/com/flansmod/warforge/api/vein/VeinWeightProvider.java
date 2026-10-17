package com.flansmod.warforge.api.vein;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.ToDoubleFunction;

/**
 * Position-dependent vein weights, installed through {@code WarforgeAPI.setVeinWeightProvider}.
 *
 * <p>Consulted once per chunk, the first time its vein is rolled; the result is persisted like any other roll.
 * Chunks rolled before the provider was installed keep their vein until rerolled.</p>
 */
@FunctionalInterface
public interface VeinWeightProvider {

    /**
     * Multiplier on each vein's dimension weight for this chunk; the empty-chunk weight is never scaled.
     * {@code null} => stock roll, megachunk quotas included. Non-null => weighted roll, quotas bypassed.
     */
    @Nullable
    ToDoubleFunction<Vein> multipliers(ResourceKey<Level> dim, int chunkX, int chunkZ);

    /** Vein decided for this chunk ahead of any roll or gradient gate; {@code null} => roll as usual. */
    @Nullable
    default Assignment assigned(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        return null;
    }

    /** Quality shifts by {@code round(log2(richness))} levels, clamped. Applies to weighted rolls only. */
    default double richness(ResourceKey<Level> dim, int chunkX, int chunkZ, Vein vein) {
        return 1.0;
    }

    /** {@code vein} null => chunk forced empty; quality shifted by richness as in {@link #richness}. */
    record Assignment(@Nullable Vein vein, double richness) {}

    static Quality shift(int qualityIndex, double richness) {
        int steps = richness <= 0.0 ? -Quality.values().length : (int) Math.round(Math.log(richness) / Math.log(2.0));
        return Quality.getQuality(Math.max(0, Math.min(Quality.values().length - 1, qualityIndex + steps)));
    }
}
