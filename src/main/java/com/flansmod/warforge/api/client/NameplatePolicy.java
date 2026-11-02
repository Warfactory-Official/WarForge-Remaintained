package com.flansmod.warforge.api.client;

import net.minecraft.world.entity.Entity;

/** Decides how a faction name plate is drawn; set through {@link WarforgeClientApi#setNameplatePolicy}. */
@FunctionalInterface
public interface NameplatePolicy {
    enum Mode { HIDDEN, NORMAL, SEE_THROUGH }

    /** Called per name plate per frame: must not allocate. */
    Mode modeFor(Entity entity);
}
