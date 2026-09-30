package com.flansmod.warforge.api.modularui;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** A tiny 6x6 cross centred in the area (the close button icon). */
public final class CrossTexture implements IGuiTexture {
    public static final CrossTexture INSTANCE = new CrossTexture();
    private static final int SIZE = 6;

    private CrossTexture() {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        int left = (int) x + (width - SIZE) / 2;
        int top = (int) y + (height - SIZE) / 2;
        for (int i = 0; i < SIZE; i++) {
            graphics.fill(left + i, top + i, left + i + 1, top + i + 1, 0xFFFFFFFF);
            graphics.fill(left + SIZE - 1 - i, top + i, left + SIZE - i, top + i + 1, 0xFFFFFFFF);
        }
    }
}
