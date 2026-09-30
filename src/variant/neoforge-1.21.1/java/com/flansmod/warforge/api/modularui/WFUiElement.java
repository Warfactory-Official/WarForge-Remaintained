package com.flansmod.warforge.api.modularui;

import com.flansmod.warforge.client.render.Gui2D;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

/**
 * An LDLib2 element that draws its contents through Gui2D. The LDLib2 draw hook differs between versions (an
 * overridden method here, a registered element renderer in the 26.x builds).
 */
public abstract class WFUiElement extends UIElement {
    /** Draws the element's contents, after its background. */
    protected abstract void draw(Gui2D graphics);

    @Override
    public void drawContents(GUIContext ctx) {
        super.drawContents(ctx);
        draw(Gui2D.of(ctx.graphics));
    }
}
