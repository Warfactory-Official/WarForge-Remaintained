package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** NativeImage pixel access and texture creation. The pixel format of the accessors changed in 1.21.2. */
public final class Images {
    private Images() {}

    /** The pixel as ABGR (alpha, blue, green, red from high to low byte). */
    public static int getPixelABGR(NativeImage image, int x, int y) {
        return swapRedBlue(image.getPixel(x, y));
    }

    /** Sets the pixel from ABGR (alpha, blue, green, red from high to low byte). */
    public static void setPixelABGR(NativeImage image, int x, int y, int abgr) {
        image.setPixel(x, y, swapRedBlue(abgr));
    }

    private static int swapRedBlue(int color) {
        return (color & 0xFF00FF00) | ((color & 0x00FF0000) >>> 16) | ((color & 0x000000FF) << 16);
    }

    static AbstractTexture dynamicTexture(Identifier id, NativeImage image) {
        return new DynamicTexture(id::toString, image);
    }
}
