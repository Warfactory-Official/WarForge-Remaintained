package com.flansmod.warforge.api.modularui;

import com.flansmod.warforge.client.render.Gui2D;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;

/**
 * An LDLib2 element that draws its contents through Gui2D. The LDLib2 draw hook differs between versions (an
 * overridden method before, a registered element renderer here).
 */
public abstract class WFUiElement extends UIElement {
    static {
        // Registered before the first element exists, so the renderer lookup never caches another renderer for it.
        UIElementRendererRegistry.register(WFUiElement.class, new UIElementRenderer<WFUiElement>() {
            @Override
            public void drawContents(WFUiElement element, IGUIContext ctx) {
                UIElementRendererRegistry.defaultRenderer().drawContents(element, ctx);
                if (ctx instanceof GUIContext guiContext) {
                    element.draw(Gui2D.of(guiContext.graphics));
                }
            }

            @Override
            public void drawBackgroundAdditional(WFUiElement element, IGUIContext ctx) {
                UIElementRendererRegistry.defaultRenderer().drawBackgroundAdditional(element, ctx);
            }

            @Override
            public void drawBackgroundOverlay(WFUiElement element, IGUIContext ctx) {
                UIElementRendererRegistry.defaultRenderer().drawBackgroundOverlay(element, ctx);
            }
        });
    }

    /** Draws the element's contents, after its background. */
    protected abstract void draw(Gui2D graphics);
}
