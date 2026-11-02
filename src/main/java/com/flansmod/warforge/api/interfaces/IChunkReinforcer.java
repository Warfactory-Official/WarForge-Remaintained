package com.flansmod.warforge.api.interfaces;

/**
 * Reinforces claimed chunks within range (Chebyshev, chunks) while running; queried via {@code WarForgeCapabilities.CHUNK_REINFORCER}
 * so it works across capability boundaries. Contributes only when its own chunk is a loaded claim of the defender.
 */
public interface IChunkReinforcer {
    /** Whether the reinforcer is currently running and should contribute defence. */
    boolean isReinforcementActive();

    /** Reinforcement range in chunks (Chebyshev distance from this tile's chunk). 0 = only this chunk. */
    int getReinforcementRadius();

    /** Extra siege difficulty added to each covered claimed chunk while active. */
    int getReinforcementBonus();

    /**
     * If {@code false} (default), only the single strongest non-stacking reinforcer covering a besieged chunk
     * applies. If {@code true}, this reinforcer's bonus adds on top of every other contribution.
     */
    default boolean stacksWithOthers() {
        return false;
    }
}
