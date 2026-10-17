package com.flansmod.warforge.client.render;

import com.flansmod.warforge.client.ClientFlagRegistry;
import com.flansmod.warforge.client.util.SkinUtil;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.util.UUID;

/** Drawing of the custom UI widgets (flag, player face, colour picker). Both UI libraries draw through these. */
public final class UiPainters {
    private static final int PICKER_BORDER = 0xFF11161A;

    private UiPainters() {
    }

    /** A flag scaled to fit the box, or an empty frame if the flag is unknown. */
    public static void flag(Gui2D graphics, String flagId, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        ResourceLocation texture = flagId == null ? null : ClientFlagRegistry.getFlagTexture(flagId);
        int[] dims = flagId == null ? null : ClientFlagRegistry.getFlagDimensions(flagId);
        if (texture == null || dims == null || dims[0] <= 0 || dims[1] <= 0) {
            graphics.fill(x, y, x + width, y + height, 0xFF2D3338);
            graphics.fill(x, y, x + width, y + 1, 0xFF11161A);
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFF11161A);
            graphics.fill(x, y, x + 1, y + height, 0xFF11161A);
            graphics.fill(x + width - 1, y, x + width, y + height, 0xFF11161A);
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

    /** The colour at a fraction (0..1) along the bar of one HSB channel (0 hue, 1 saturation, 2 brightness). */
    public static int channelColour(float[] hsb, int channel, float fraction) {
        if (channel == 0) {
            return Color.HSBtoRGB(fraction, 1f, 1f);
        }
        if (channel == 1) {
            return Color.HSBtoRGB(hsb[0], fraction, 1f);
        }
        return Color.HSBtoRGB(hsb[0], hsb[1], fraction);
    }

    /** One channel bar of the colour picker, with the knob at the channel's current value. */
    public static void colourBar(Gui2D graphics, float[] hsb, int channel, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        for (int n = 0; n < width; n++) {
            float fraction = width <= 1 ? 0f : n / (float) (width - 1);
            int rgb = channelColour(hsb, channel, fraction);
            graphics.fill(x + n, y, x + n + 1, y + height, 0xFF000000 | (rgb & 0x00FFFFFF));
        }
        graphics.fill(x, y, x + width, y + 1, PICKER_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, PICKER_BORDER);
        int knobX = x + Math.round(hsb[channel] * (width - 1));
        graphics.fill(knobX - 1, y - 1, knobX + 2, y + height + 1, 0xFF000000);
        graphics.fill(knobX, y - 1, knobX + 1, y + height + 1, 0xFFFFFFFF);
    }

    /** The colour picker's preview swatch. */
    public static void colourSwatch(Gui2D graphics, float[] hsb, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        graphics.fill(x, y, x + width, y + height, 0xFF000000 | (rgb & 0x00FFFFFF));
        graphics.fill(x, y, x + width, y + 1, PICKER_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, PICKER_BORDER);
        graphics.fill(x, y, x + 1, y + height, PICKER_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, PICKER_BORDER);
    }
}
