package com.flansmod.warforge.compat;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** ResourceKey.location() -> identifier() in 1.21.11. */
public final class Keys {
    private Keys() {}

    public static Identifier id(ResourceKey<?> key) {
        return key.identifier();
    }
}
