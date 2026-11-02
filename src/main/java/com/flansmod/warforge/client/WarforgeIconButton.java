package com.flansmod.warforge.client;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.WFButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.flansmod.warforge.compat.Ids;

public final class WarforgeIconButton extends WFButton {
    public static final ResourceLocation WARFORGE_BUTTONS_TEXTURE = Ids.of(Tags.MODID, "gui/icon_claim.png");
    public static final int WARFORGE_BUTTON_SIZE = 18;
    private static final int HOVER_HIGHLIGHT = 0x80FFFFFF;

    private final int textureX;
    private final Runnable handler;

    /** @param textureX left edge of the icon in the button texture; label is a translation key */
    public WarforgeIconButton(int x, int y, int textureX, String labelKey, Runnable handler) {
        super(x, y, WARFORGE_BUTTON_SIZE, WARFORGE_BUTTON_SIZE, CommonComponents.EMPTY);
        this.textureX = textureX;
        this.handler = handler;
        setTooltip(Tooltip.create(Component.translatable(labelKey)));
    }

    @Override
    public void onPress() {
        handler.run();
    }

    @Override
    protected void drawButton(Gui2D graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(WARFORGE_BUTTONS_TEXTURE, getX(), getY(), textureX, 0, WARFORGE_BUTTON_SIZE, WARFORGE_BUTTON_SIZE, 256, 256);
        if (isHoveredOrFocused()) {
            graphics.fill(getX(), getY(), getX() + WARFORGE_BUTTON_SIZE, getY() + WARFORGE_BUTTON_SIZE, HOVER_HIGHLIGHT);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
