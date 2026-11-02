package com.flansmod.warforge.compat;

import net.minecraft.network.chat.Component;

public final class ChatJson {
    private ChatJson() {}

    public static String toJson(Component component) {
        return Component.Serializer.toJson(component);
    }

    public static Component fromJson(String json) {
        return Component.Serializer.fromJson(json);
    }
}
