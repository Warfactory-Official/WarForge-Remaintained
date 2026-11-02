package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class Gui2D {
    private static final Gui2D SHARED = new Gui2D();
    private static final float QUARTER_TURN = (float) (Math.PI / 2);

    private GuiGraphics graphics;

    private Gui2D() {
    }

    public static Gui2D of(GuiGraphics graphics) {
        SHARED.graphics = graphics;
        return SHARED;
    }

    public GuiGraphics raw() {
        return graphics;
    }

    public void fill(int x0, int y0, int x1, int y1, int argb) {
        graphics.fill(x0, y0, x1, y1, argb);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int argbTop, int argbBottom) {
        graphics.fillGradient(x0, y0, x1, y1, argbTop, argbBottom);
    }

    public void fillGradientH(int x0, int y0, int x1, int y1, int argbLeft, int argbRight) {
        graphics.pose().pushPose();
        graphics.pose().translate(x0, y1, 0);
        graphics.pose().last().pose().rotateZ(-QUARTER_TURN);
        graphics.fillGradient(0, 0, y1 - y0, x1 - x0, argbLeft, argbRight);
        graphics.pose().popPose();
    }

    public void border(int x0, int y0, int x1, int y1, int thickness, int argb) {
        fill(x0, y0, x1, y0 + thickness, argb);
        fill(x0, y1 - thickness, x1, y1, argb);
        fill(x0, y0 + thickness, x0 + thickness, y1 - thickness, argb);
        fill(x1 - thickness, y0 + thickness, x1, y1 - thickness, argb);
    }

    public void blit(ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texWidth, int texHeight) {
        graphics.blit(texture, x, y, u, v, w, h, texWidth, texHeight);
    }

    public void blit(ResourceLocation texture, int x, int y, int w, int h, float u, float v, int uWidth, int vHeight,
                     int texWidth, int texHeight) {
        graphics.blit(texture, x, y, w, h, u, v, uWidth, vHeight, texWidth, texHeight);
    }

    public void text(Font font, String text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

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

    public void playerFace(ResourceLocation skin, int x, int y, int size) {
        PlayerFaceRenderer.draw(graphics, skin, x, y, size);
    }

    public void setColor(float red, float green, float blue, float alpha) {
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    public void enableBlend() {
        RenderSystem.enableBlend();
    }

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
