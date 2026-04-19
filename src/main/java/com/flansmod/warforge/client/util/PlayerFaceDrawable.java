package com.flansmod.warforge.client.util;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

public class PlayerFaceDrawable implements IDrawable {
    private final UUID playerId;

    public PlayerFaceDrawable(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
        ResourceLocation face = SkinUtil.getPlayerFace(playerId);
        Minecraft.getMinecraft().getTextureManager().bindTexture(face);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 8, 8, width, height, 8, 8);
    }
}
