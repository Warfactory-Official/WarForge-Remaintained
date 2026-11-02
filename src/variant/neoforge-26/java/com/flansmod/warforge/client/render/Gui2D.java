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

/** 26.x has no shader colour: setColor keeps a colour and passes it to blits and faces; fills and text stay untinted. */
public final class Gui2D {
    private static final Gui2D SHARED = new Gui2D();
    private static final float QUARTER_TURN = (float) (Math.PI / 2);

    private GuiGraphicsExtractor graphics;
    private int color = -1;

    private Gui2D() {
    }

    public static Gui2D of(GuiGraphicsExtractor graphics) {
        SHARED.graphics = graphics;
        SHARED.color = -1;
        return SHARED;
    }

    public GuiGraphicsExtractor raw() {
        return graphics;
    }

    public void fill(int x0, int y0, int x1, int y1, int argb) {
        graphics.fill(x0, y0, x1, y1, argb);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int argbTop, int argbBottom) {
        graphics.fillGradient(x0, y0, x1, y1, argbTop, argbBottom);
    }

    public void fillGradientH(int x0, int y0, int x1, int y1, int argbLeft, int argbRight) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x0, y1);
        graphics.pose().rotate(-QUARTER_TURN);
        graphics.fillGradient(0, 0, y1 - y0, x1 - x0, argbLeft, argbRight);
        graphics.pose().popMatrix();
    }

    public void border(int x0, int y0, int x1, int y1, int thickness, int argb) {
        fill(x0, y0, x1, y0 + thickness, argb);
        fill(x0, y1 - thickness, x1, y1, argb);
        fill(x0, y0 + thickness, x0 + thickness, y1 - thickness, argb);
        fill(x1 - thickness, y0 + thickness, x1, y1 - thickness, argb);
    }

    public void blit(Identifier texture, int x, int y, float u, float v, int w, int h, int texWidth, int texHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, texWidth, texHeight, color);
    }

    public void blit(Identifier texture, int x, int y, int w, int h, float u, float v, int uWidth, int vHeight,
                     int texWidth, int texHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, uWidth, vHeight, texWidth, texHeight, color);
    }

    public void text(Font font, String text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

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

    public void playerFace(Identifier skin, int x, int y, int size) {
        PlayerFaceExtractor.extractRenderState(graphics, skin, x, y, size, true, false, color);
    }

    public void setColor(float red, float green, float blue, float alpha) {
        color = ARGB.colorFromFloat(alpha, red, green, blue);
    }

    public void enableBlend() {
    }

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

    /** Alpha < 4 drew opaque before 1.21.6; 26.x draws it invisible. */
    private static int textColor(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
}
