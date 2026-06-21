package com.flansmod.warforge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;

/**
 * A {@link Button} that occupies space and reacts to clicks but never draws anything. Used by
 * {@link GuiFactionInfo} to make the player face tiles clickable without painting a vanilla button frame.
 */
public class GuiInvisibleButton extends Button {

    public GuiInvisibleButton(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Intentionally renders nothing.
    }
}
