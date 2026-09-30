package com.flansmod.warforge.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** Base class of WarForge buttons. The vanilla widget render method differs between versions. */
public abstract class WFButton extends AbstractButton {
    protected WFButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    /** Draws the button. */
    protected abstract void drawButton(Gui2D graphics, int mouseX, int mouseY, float partialTicks);

    /** Called when the button is clicked or activated with a key. */
    public abstract void onPress();

    @Override
    public final void onPress(InputWithModifiers input) {
        onPress();
    }

    @Override
    protected final void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        drawButton(Gui2D.of(graphics), mouseX, mouseY, partialTicks);
    }
}
