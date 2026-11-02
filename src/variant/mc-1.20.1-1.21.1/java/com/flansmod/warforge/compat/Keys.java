package com.flansmod.warforge.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** ResourceKey.location() -> identifier() in 1.21.11. */
public final class Keys {
    private Keys() {}

    public static ResourceLocation id(ResourceKey<?> key) {
        return key.location();
    }
}
