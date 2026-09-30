package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Textures the mod makes at runtime (flags, map cells, item icons). Call from the render thread. */
public final class DynamicTextures {
    private static final Set<ResourceLocation> REGISTERED = ConcurrentHashMap.newKeySet();

    private DynamicTextures() {
    }

    /** Uploads the image as a texture at id (replacing an older one). The texture owns the image afterwards. */
    public static void register(ResourceLocation id, NativeImage image) {
        Minecraft.getInstance().getTextureManager().register(id, Images.dynamicTexture(id, image));
        REGISTERED.add(id);
    }

    public static void release(ResourceLocation id) {
        Minecraft.getInstance().getTextureManager().release(id);
        REGISTERED.remove(id);
    }

    /** True if a dynamic texture is registered at id (without loading anything). */
    public static boolean isRegistered(ResourceLocation id) {
        return REGISTERED.contains(id);
    }
}
