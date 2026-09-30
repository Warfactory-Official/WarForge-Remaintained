package com.flansmod.warforge.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;

/** Base class of WarForge buttons. The vanilla widget render method differs between versions. */
public abstract class WFButton extends AbstractButton {
    protected WFButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    /** Draws the button. */
    protected abstract void drawButton(Gui2D graphics, int mouseX, int mouseY, float partialTicks);

    @Override
    public final void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawButton(Gui2D.of(graphics), mouseX, mouseY, partialTicks);
    }
}
