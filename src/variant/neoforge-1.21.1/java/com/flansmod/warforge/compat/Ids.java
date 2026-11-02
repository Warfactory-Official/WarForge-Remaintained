package com.flansmod.warforge.compat;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class Ids {
    private Ids() {}

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation parse(String id) {
        return ResourceLocation.parse(id);
    }

    public static @Nullable ResourceLocation tryParse(String id) {
        return ResourceLocation.tryParse(id);
    }
}
