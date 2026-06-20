package com.flansmod.warforge.api.interfaces;

import net.minecraft.util.math.Vec3i;

/**
 * @deprecated since 2.1.0. Use {@link IChunkReinforcer} exposed through
 * {@code WarForgeCapabilities.CHUNK_REINFORCER} instead. The capability is queried generically and works
 * across capability boundaries (e.g. GregTech MetaTileEntities), whereas this interface only worked for tiles
 * that {@code instanceof}-matched directly. This interface is still honoured by
 * {@code WarforgeAPI.getReinforcementBonus} for backwards compatibility.
 */
@Deprecated
public interface IClaimStrengthModifier {

    int getClaimContribution();

    Vec3i[] getEffectArea();

    boolean isActive();

    default boolean canStack() {
        return false;
    }
}
