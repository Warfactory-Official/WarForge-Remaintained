package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Render thread only. */
public final class DynamicTextures {
    private static final Set<ResourceLocation> REGISTERED = ConcurrentHashMap.newKeySet();

    private DynamicTextures() {
    }

    public static void register(ResourceLocation id, NativeImage image) {
        Minecraft.getInstance().getTextureManager().register(id, Images.dynamicTexture(id, image));
        REGISTERED.add(id);
    }

    public static void release(ResourceLocation id) {
        Minecraft.getInstance().getTextureManager().release(id);
        REGISTERED.remove(id);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return REGISTERED.contains(id);
    }
}
