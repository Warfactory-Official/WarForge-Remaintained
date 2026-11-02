package com.flansmod.warforge.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public final class ChatJson {
    private ChatJson() {}

    public static String toJson(Component component) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).result().orElseThrow().toString();
    }

    public static Component fromJson(String json) {
        JsonElement element = JsonParser.parseString(json);
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element).result().orElseThrow();
    }
}
