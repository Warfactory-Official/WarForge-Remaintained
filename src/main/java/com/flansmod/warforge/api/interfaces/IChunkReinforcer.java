package com.flansmod.warforge.api.interfaces;

/**
 * Capability exposed by tiles that reinforce a faction's claimed chunks, raising the siege difficulty of any
 * claimed chunk within range while the tile is running. Replaces the older {@link IClaimStrengthModifier}:
 * it is queried through {@code WarForgeCapabilities.CHUNK_REINFORCER} rather than {@code instanceof}, so it
 * works across capability boundaries (e.g. GregTech MetaTileEntities, whose in-world tile is a holder).
 *
 * <p>The radius is measured in chunks as a Chebyshev (square) distance from the reinforcer's own chunk. A
 * reinforcer only contributes when its own chunk is a loaded claim of the defending faction.
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
