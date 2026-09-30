package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** NativeImage pixel access and texture creation. The pixel format of the accessors changed in 1.21.2. */
public final class Images {
    private Images() {}

    /** The pixel as ABGR (alpha, blue, green, red from high to low byte). */
    public static int getPixelABGR(NativeImage image, int x, int y) {
        return image.getPixelRGBA(x, y);
    }

    /** Sets the pixel from ABGR (alpha, blue, green, red from high to low byte). */
    public static void setPixelABGR(NativeImage image, int x, int y, int abgr) {
        image.setPixelRGBA(x, y, abgr);
    }

    static AbstractTexture dynamicTexture(ResourceLocation id, NativeImage image) {
        return new DynamicTexture(image);
    }
}
