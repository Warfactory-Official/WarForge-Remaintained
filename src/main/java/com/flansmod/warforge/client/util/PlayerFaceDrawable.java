package com.flansmod.warforge.client.util;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.UUID;

public class PlayerFaceDrawable implements IDrawable {
    private final UUID playerId;

    public PlayerFaceDrawable(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        GuiGraphics graphics = context.getGraphics();
        graphics.flush();
        SkinUtil.drawFace(graphics, playerId, x, y, Math.min(width, height));
    }
}
