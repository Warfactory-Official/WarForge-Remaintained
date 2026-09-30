package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

/** Key mappings. The category type changed in 1.21.9 (a string before, a registered record after). */
public final class KeyMaps {
    private KeyMaps() {}

    /** An in-game key mapping with a keyboard default. category is the category translation key. */
    public static KeyMapping create(String name, int glfwKey, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, glfwKey, category);
    }

    public static void register(IEventBus modBus, KeyMapping... keys) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            for (KeyMapping key : keys) {
                event.register(key);
            }
        });
    }
}
