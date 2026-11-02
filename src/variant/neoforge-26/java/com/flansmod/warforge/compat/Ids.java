package com.flansmod.warforge.compat;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public final class Ids {
    private Ids() {}

    public static Identifier of(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier parse(String id) {
        return Identifier.parse(id);
    }

    public static @Nullable Identifier tryParse(String id) {
        return Identifier.tryParse(id);
    }
}
