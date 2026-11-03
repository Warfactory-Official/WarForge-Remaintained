package com.flansmod.warforge.api;

import net.minecraft.world.damagesource.DamageSource;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Registered through {@link WarforgeAPI#addKillAttributor}; consulted on the server thread when a player dies to a
 * source whose {@link DamageSource#getEntity()} is not a player (unmanned vehicle, drone, offline mine layer).
 */
@FunctionalInterface
public interface KillAttributor {
    /** @return the faction credited with the kill, or null */
    @Nullable
    UUID factionOf(DamageSource source);
}
