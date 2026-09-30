package com.flansmod.warforge.compat;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/** Resource id factories. The static factories on ResourceLocation differ between versions. */
public final class Ids {
    private Ids() {}

    public static Identifier of(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    /** Parses "namespace:path" (or "path", which means minecraft:path). Throws on an invalid id. */
    public static Identifier parse(String id) {
        return Identifier.parse(id);
    }

    /** Parses "namespace:path" (or "path", which means minecraft:path). Returns null on an invalid id. */
    public static @Nullable Identifier tryParse(String id) {
        return Identifier.tryParse(id);
    }
}
