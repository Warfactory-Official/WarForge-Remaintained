package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.util.LinkedHashMap;
import java.util.Map;

/** Key mappings. The category type changed in 1.21.9 (a string before, a registered record after). */
public final class KeyMaps {
    private static final Map<String, KeyMapping.Category> CATEGORIES = new LinkedHashMap<>();

    private KeyMaps() {}

    /**
     * An in-game key mapping with a keyboard default. category is the category translation key of older versions
     * ("key.NAMESPACE.PATH"). Here it becomes the category NAMESPACE:PATH, whose label key is
     * "key.category.NAMESPACE.PATH".
     */
    public static KeyMapping create(String name, int glfwKey, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, glfwKey,
                CATEGORIES.computeIfAbsent(category, KeyMaps::toCategory));
    }

    public static void register(IEventBus modBus, KeyMapping... keys) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            for (KeyMapping.Category category : CATEGORIES.values()) {
                event.registerCategory(category);
            }
            for (KeyMapping key : keys) {
                event.register(key);
            }
        });
    }

    private static KeyMapping.Category toCategory(String key) {
        String rest = key.startsWith("key.") ? key.substring(4) : key;
        int dot = rest.indexOf('.');
        Identifier id = dot < 0 ? Identifier.withDefaultNamespace(rest)
                : Identifier.fromNamespaceAndPath(rest.substring(0, dot), rest.substring(dot + 1));
        return new KeyMapping.Category(id);
    }
}
