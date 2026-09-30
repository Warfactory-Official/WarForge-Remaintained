package com.flansmod.warforge.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * 2D drawing for the HUD, toasts and UI widgets. Wraps the version's GUI graphics object and offers the operations
 * the mod uses, so the drawing code is the same on every version.
 *
 * <p>26.x has no shader colour. setColor keeps a colour here and passes it to the blits and faces. These are the
 * draws that the shader colour tinted in 1.21.1 (fills and text were batched and drawn later, untinted).
 */
public final class Gui2D {
    private final GuiGraphicsExtractor graphics;
    private int color = -1;

    private Gui2D(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    public static Gui2D of(GuiGraphicsExtractor graphics) {
        return new Gui2D(graphics);
    }

    /** The wrapped vanilla graphics object. */
    public GuiGraphicsExtractor raw() {
        return graphics;
    }

    public void fill(int x0, int y0, int x1, int y1, int argb) {
        graphics.fill(x0, y0, x1, y1, argb);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int argbTop, int argbBottom) {
        graphics.fillGradient(x0, y0, x1, y1, argbTop, argbBottom);
    }

    /** Draws a w x h region of the texture starting at (u, v), 1:1. */
    public void blit(Identifier texture, int x, int y, float u, float v, int w, int h, int texWidth, int texHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, texWidth, texHeight, color);
    }

    /** Draws the uWidth x vHeight region of the texture at (u, v), scaled to w x h. */
    public void blit(Identifier texture, int x, int y, int w, int h, float u, float v, int uWidth, int vHeight,
                     int texWidth, int texHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, uWidth, vHeight, texWidth, texHeight, color);
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
        graphics.text(font, text, x, y, textColor(color), shadow);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, textColor(color), shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, textColor(color), shadow);
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.item(stack, x, y);
    }

    /** Draws a player's face (with hat layer) from their skin texture. */
    public void playerFace(Identifier skin, int x, int y, int size) {
        PlayerFaceExtractor.extractRenderState(graphics, skin, x, y, size, true, false, color);
    }

    /** Multiplies the colour of the following texture draws. Reset with setColor(1, 1, 1, 1). */
    public void setColor(float red, float green, float blue, float alpha) {
        color = ARGB.colorFromFloat(alpha, red, green, blue);
    }

    /** Enables alpha blending for the following texture draws. The 26.x GUI pipelines always blend. */
    public void enableBlend() {
    }

    /** Draws everything queued so far. The 26.x GUI keeps the draw order itself, so there is nothing to do. */
    public void flush() {
    }

    public void push() {
        graphics.pose().pushMatrix();
    }

    public void pop() {
        graphics.pose().popMatrix();
    }

    public void translate(float x, float y) {
        graphics.pose().translate(x, y);
    }

    public void scale(float x, float y) {
        graphics.pose().scale(x, y);
    }

    /** Before 1.21.6 a text colour with an alpha below 4 was drawn opaque. 26.x draws it invisible. */
    private static int textColor(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
}
