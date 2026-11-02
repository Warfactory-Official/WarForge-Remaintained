package com.flansmod.warforge.api.modularui;

import com.flansmod.warforge.client.render.Gui2D;

public class VanillaPanelElement extends WFUiElement {
    private static final int OUTLINE = 0xFF000000;
    private static final int FILL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;

    @Override
    protected void draw(Gui2D graphics) {
        int x = (int) getPositionX();
        int y = (int) getPositionY();
        int w = (int) getSizeWidth();
        int h = (int) getSizeHeight();
        if (w < 8 || h < 8) {
            return;
        }
        graphics.fill(x + 2, y, x + w - 2, y + 1, OUTLINE);
        graphics.fill(x + 2, y + h - 1, x + w - 2, y + h, OUTLINE);
        graphics.fill(x, y + 2, x + 1, y + h - 2, OUTLINE);
        graphics.fill(x + w - 1, y + 2, x + w, y + h - 2, OUTLINE);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, OUTLINE);
        graphics.fill(x + w - 2, y + 1, x + w - 1, y + 2, OUTLINE);
        graphics.fill(x + 1, y + h - 2, x + 2, y + h - 1, OUTLINE);
        graphics.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, OUTLINE);
        graphics.fill(x + 2, y + 1, x + w - 2, y + h - 1, FILL);
        graphics.fill(x + 1, y + 2, x + w - 1, y + h - 2, FILL);
        graphics.fill(x + 2, y + 1, x + w - 3, y + 3, LIGHT);
        graphics.fill(x + 1, y + 2, x + 3, y + h - 3, LIGHT);
        graphics.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SHADOW);
        graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SHADOW);
    }
}
