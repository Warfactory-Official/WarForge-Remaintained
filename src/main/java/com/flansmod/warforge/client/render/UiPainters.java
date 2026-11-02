package com.flansmod.warforge.client.render;

import com.flansmod.warforge.client.ClientFlagRegistry;
import com.flansmod.warforge.client.util.ColorUtil;
import com.flansmod.warforge.client.util.SkinUtil;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.util.UUID;

public final class UiPainters {
    private static final int PICKER_BORDER = 0xFF11161A;
    private static final int EMPTY_FLAG_FILL = 0xFF2D3338;
    private static final int HUE_SEGMENTS = 6;
    private static final int[] HUE_STOPS = new int[HUE_SEGMENTS + 1];

    static {
        for (int i = 0; i <= HUE_SEGMENTS; i++) {
            HUE_STOPS[i] = Color.HSBtoRGB(i / (float) HUE_SEGMENTS, 1f, 1f);
        }
    }

    private UiPainters() {
    }

    public static void flag(Gui2D graphics, String flagId, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        ResourceLocation texture = flagId == null ? null : ClientFlagRegistry.getFlagTexture(flagId);
        int[] dims = flagId == null ? null : ClientFlagRegistry.getFlagDimensions(flagId);
        if (texture == null || dims == null || dims[0] <= 0 || dims[1] <= 0) {
            graphics.fill(x, y, x + width, y + height, EMPTY_FLAG_FILL);
            graphics.border(x, y, x + width, y + height, 1, PICKER_BORDER);
            return;
        }

        float scale = Math.min(width / (float) dims[0], height / (float) dims[1]);
        int drawWidth = Math.max(1, Math.round(dims[0] * scale));
        int drawHeight = Math.max(1, Math.round(dims[1] * scale));
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;

        // LDLib2 can leave the shader colour non-white => flag invisible (see SkinUtil.drawFace).
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.flush();
        graphics.blit(texture, drawX, drawY, drawWidth, drawHeight, 0f, 0f, dims[0], dims[1], dims[0], dims[1]);
        graphics.flush();
    }

    public static void playerFace(Gui2D graphics, UUID playerId, int x, int y, int size) {
        if (size <= 0) {
            return;
        }
        SkinUtil.drawFace(graphics, playerId, x, y, size);
    }

    public static int channelColour(float[] hsb, int channel, float fraction) {
        if (channel == 0) {
            return Color.HSBtoRGB(fraction, 1f, 1f);
        }
        if (channel == 1) {
            return Color.HSBtoRGB(hsb[0], fraction, 1f);
        }
        return Color.HSBtoRGB(hsb[0], hsb[1], fraction);
    }

    public static void colourBar(Gui2D graphics, float[] hsb, int channel, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (channel == 0) {
            for (int i = 0; i < HUE_SEGMENTS; i++) {
                graphics.fillGradientH(x + width * i / HUE_SEGMENTS, y, x + width * (i + 1) / HUE_SEGMENTS, y + height,
                        ColorUtil.opaque(HUE_STOPS[i]), ColorUtil.opaque(HUE_STOPS[i + 1]));
            }
        } else {
            graphics.fillGradientH(x, y, x + width, y + height, ColorUtil.opaque(channelColour(hsb, channel, 0f)), ColorUtil.opaque(channelColour(hsb, channel, 1f)));
        }
        graphics.border(x, y, x + width, y + height, 1, PICKER_BORDER);
        int knobX = x + Math.round(hsb[channel] * (width - 1));
        graphics.fill(knobX - 1, y - 1, knobX + 2, y + height + 1, 0xFF000000);
        graphics.fill(knobX, y - 1, knobX + 1, y + height + 1, 0xFFFFFFFF);
    }

    public static void colourSwatch(Gui2D graphics, float[] hsb, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        graphics.fill(x, y, x + width, y + height, ColorUtil.opaque(rgb));
        graphics.border(x, y, x + width, y + height, 1, PICKER_BORDER);
    }

}
