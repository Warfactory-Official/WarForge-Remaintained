package com.flansmod.warforge.api.modularui;

import com.flansmod.warforge.client.render.Gui2D;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

public abstract class WFUiElement extends UIElement {
    protected abstract void draw(Gui2D graphics);

    @Override
    public void drawContents(GUIContext ctx) {
        super.drawContents(ctx);
        draw(Gui2D.of(ctx.graphics));
    }
}
