package com.flansmod.warforge.client.render;

/** HUD draw callback; primitive partial tick avoids a boxed Float per frame. */
@FunctionalInterface
public interface HudHandler {
    void draw(Gui2D graphics, float partialTick);
}
