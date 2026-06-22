package com.flansmod.warforge.client.util;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;

import java.util.UUID;

public class PlayerFaceDrawable implements IDrawable {
    private final UUID playerId;

    public PlayerFaceDrawable(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        // Render through vanilla PlayerFaceRenderer on the managed GuiGraphics. The manual GuiDraw +
        // getLastGraphicsPose() path does not reliably draw dynamically-loaded skin textures (blank face),
        // and this also composites the hat overlay. Faces are square, so use the smaller dimension.
        SkinUtil.drawFace(context.getGraphics(), playerId, x, y, Math.min(width, height));
    }
}
