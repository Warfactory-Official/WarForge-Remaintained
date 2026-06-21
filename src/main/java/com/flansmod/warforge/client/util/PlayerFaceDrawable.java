package com.flansmod.warforge.client.util;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class PlayerFaceDrawable implements IDrawable {
    private final UUID playerId;

    public PlayerFaceDrawable(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        ResourceLocation skin = SkinUtil.getPlayerFace(playerId);
        // Crop the 8x8 head face out of the full skin sheet.
        GuiDraw.drawTexture(context.getLastGraphicsPose(), skin, x, y, x + width, y + height,
                SkinUtil.FACE_U0, SkinUtil.FACE_V0, SkinUtil.FACE_U1, SkinUtil.FACE_V1, true);
    }
}
