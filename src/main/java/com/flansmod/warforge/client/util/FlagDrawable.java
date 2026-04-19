package com.flansmod.warforge.client.util;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.flansmod.warforge.client.ClientFlagRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

public class FlagDrawable implements IDrawable {
    private final String flagId;

    public FlagDrawable(String flagId) {
        this.flagId = flagId;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
        ResourceLocation texture = ClientFlagRegistry.getFlagTexture(flagId);
        int[] dims = ClientFlagRegistry.getFlagDimensions(flagId);
        if (texture == null || dims == null || dims[0] <= 0 || dims[1] <= 0) {
            Gui.drawRect(x, y, x + width, y + height, 0xFF2D3338);
            Gui.drawRect(x, y, x + width, y + 1, 0xFF11161A);
            Gui.drawRect(x, y + height - 1, x + width, y + height, 0xFF11161A);
            Gui.drawRect(x, y, x + 1, y + height, 0xFF11161A);
            Gui.drawRect(x + width - 1, y, x + width, y + height, 0xFF11161A);
            return;
        }

        float scale = Math.min(width / (float) dims[0], height / (float) dims[1]);
        int drawWidth = Math.max(1, Math.round(dims[0] * scale));
        int drawHeight = Math.max(1, Math.round(dims[1] * scale));
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        Gui.drawScaledCustomSizeModalRect(drawX, drawY, 0, 0, dims[0], dims[1], drawWidth, drawHeight, dims[0], dims[1]);
    }
}
