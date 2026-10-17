package com.flansmod.warforge.api.modularui;

import com.flansmod.warforge.client.render.Gui2D;

/** ModularUI {@code gui/icons/cross_tiny} (10x10). */
class CrossElement extends WFUiElement {
    private static final int[][] PIXELS = {{2, 2}, {7, 2}, {3, 3}, {6, 3}, {4, 4}, {5, 4}, {4, 5}, {5, 5}, {3, 6}, {6, 6}, {2, 7}, {7, 7}};

    @Override
    protected void draw(Gui2D graphics) {
        int x = (int) getPositionX();
        int y = (int) getPositionY();
        for (int[] p : PIXELS) {
            graphics.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, 0xFFFFFFFF);
        }
    }
}
