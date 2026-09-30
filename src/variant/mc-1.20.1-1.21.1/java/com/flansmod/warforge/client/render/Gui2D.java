package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * 2D drawing for the HUD, toasts and UI widgets. Wraps the version's GUI graphics object and offers the operations
 * the mod uses, so the drawing code is the same on every version.
 */
public final class Gui2D {
    private final GuiGraphics graphics;

    private Gui2D(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public static Gui2D of(GuiGraphics graphics) {
        return new Gui2D(graphics);
    }

    /** The wrapped vanilla graphics object. */
    public GuiGraphics raw() {
        return graphics;
    }

    public void fill(int x0, int y0, int x1, int y1, int argb) {
        graphics.fill(x0, y0, x1, y1, argb);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int argbTop, int argbBottom) {
        graphics.fillGradient(x0, y0, x1, y1, argbTop, argbBottom);
    }

    /** Draws a w x h region of the texture starting at (u, v), 1:1. */
    public void blit(ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texWidth, int texHeight) {
        graphics.blit(texture, x, y, u, v, w, h, texWidth, texHeight);
    }

    /** Draws the uWidth x vHeight region of the texture at (u, v), scaled to w x h. */
    public void blit(ResourceLocation texture, int x, int y, int w, int h, float u, float v, int uWidth, int vHeight,
                     int texWidth, int texHeight) {
        graphics.blit(texture, x, y, w, h, u, v, uWidth, vHeight, texWidth, texHeight);
    }

    /** Text with a shadow. */
    public void text(Font font, String text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

    /** Text with a shadow. */
    public void text(Font font, Component text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
    }

    /** Draws a player's face (with hat layer) from their skin texture. */
    public void playerFace(ResourceLocation skin, int x, int y, int size) {
        PlayerFaceRenderer.draw(graphics, skin, x, y, size);
    }

    /** Multiplies the colour of the following draws (textures and fills). Reset with setColor(1, 1, 1, 1). */
    public void setColor(float red, float green, float blue, float alpha) {
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    /** Enables alpha blending for the following texture draws. */
    public void enableBlend() {
        RenderSystem.enableBlend();
    }

    /** Draws everything queued so far (needed before changing render state between batched draws). */
    public void flush() {
        graphics.flush();
    }

    public void push() {
        graphics.pose().pushPose();
    }

    public void pop() {
        graphics.pose().popPose();
    }

    public void translate(float x, float y) {
        graphics.pose().translate(x, y, 0);
    }

    public void scale(float x, float y) {
        graphics.pose().scale(x, y, 1f);
    }
}
