package com.flansmod.warforge.server;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Translatable player-facing text under {@code warforge.server.*}. */
public final class ServerText {
    private static final String PREFIX = "warforge.server.";

    private ServerText() {
    }

    public static MutableComponent t(String key, Object... args) {
        return Component.translatable(PREFIX + key, args);
    }
}
