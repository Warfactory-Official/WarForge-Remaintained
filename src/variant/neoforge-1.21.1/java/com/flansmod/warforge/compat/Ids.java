package com.flansmod.warforge.compat;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Resource id factories. The static factories on ResourceLocation differ between versions. */
public final class Ids {
    private Ids() {}

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /** Parses "namespace:path" (or "path", which means minecraft:path). Throws on an invalid id. */
    public static ResourceLocation parse(String id) {
        return ResourceLocation.parse(id);
    }

    /** Parses "namespace:path" (or "path", which means minecraft:path). Returns null on an invalid id. */
    public static @Nullable ResourceLocation tryParse(String id) {
        return ResourceLocation.tryParse(id);
    }
}
