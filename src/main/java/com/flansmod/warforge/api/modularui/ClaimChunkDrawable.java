package com.flansmod.warforge.api.modularui;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

public class ClaimChunkDrawable implements IDrawable, Interactable {
    private final ClaimChunkInfo info;

    public ClaimChunkDrawable(ClaimChunkInfo info) {
        this.info = info;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
        boolean hovered = context.getMouseX() >= x && context.getMouseX() < x + width
                && context.getMouseY() >= y && context.getMouseY() < y + height;

        int fillColor = 0x55101010;
        if (!info.factionId.equals(Faction.nullUuid)) {
            fillColor = 0x66000000 | (info.colour & 0x00FFFFFF);
        }
        Gui.drawRect(x, y, x + width, y + height, fillColor);

        int borderColor = hovered ? 0xFFDDDDDD : 0x55222222;
        Gui.drawRect(x, y, x + width, y + 1, borderColor);
        Gui.drawRect(x, y + height - 1, x + width, y + height, borderColor);
        Gui.drawRect(x, y, x + 1, y + height, borderColor);
        Gui.drawRect(x + width - 1, y, x + width, y + height, borderColor);

        if (info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED)) {
            Minecraft.getMinecraft().fontRenderer.drawString("F", x + 2, y + 2, 0xFFFFFF);
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_HAS_COLLECTOR)) {
            Minecraft.getMinecraft().fontRenderer.drawString("C", x + width - 7, y + 2, 0xFFFFFF);
        }
    }
}
