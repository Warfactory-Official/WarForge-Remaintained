package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.flansmod.warforge.client.util.FlagDrawable;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionFlagSelectGuiData;
import com.flansmod.warforge.common.network.PacketChooseFactionFlag;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.TextFormatting;

public final class GuiFactionFlagSelect {
    private static final int WIDTH = 332;
    private static final int HEIGHT = 248;
    private static final int CONTENT_LEFT = 12;
    private static final int BODY_Y = 54;

    private GuiFactionFlagSelect() {
    }

    public static ModularPanel buildPanel(FactionFlagSelectGuiData data) {
        ModularPanel panel = ModularPanel.defaultPanel("faction_flag_select")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(0, 0, WIDTH, 40, 0xFF171B1F, 0xFF0D1013)).size(WIDTH, 40));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, BODY_Y, WIDTH - CONTENT_LEFT * 2, HEIGHT - BODY_Y - 12, 0xEE20262B, 0xEE11161A)).size(WIDTH - CONTENT_LEFT * 2, HEIGHT - BODY_Y - 12).pos(CONTENT_LEFT, BODY_Y));
        panel.child(new IDrawable.DrawableWidget(colorStripe(0xFF000000 | (data.factionColor & 0x00FFFFFF), 0, 0, 6, HEIGHT)).size(6, HEIGHT));
        panel.child(ButtonWidget.panelCloseButton().pos(WIDTH - 18, 8));

        panel.child(IKey.str("Faction Flag").asWidget().pos(CONTENT_LEFT, 12).style(TextFormatting.BOLD).color(0xC7CCD1).shadow(true).scale(1.15f));
        panel.child(IKey.str(data.factionName).asWidget().pos(CONTENT_LEFT, 27).color(data.factionColor).style(TextFormatting.BOLD));
        panel.child(IKey.str(data.currentFlagId.isEmpty() ? "Choose once. This cannot be changed later." : "Flag locked: " + displayName(data.currentFlagId)).asWidget().pos(CONTENT_LEFT + 10, BODY_Y + 8).color(0xC7CCD1));

        ListWidget list = new ListWidget<>()
                .scrollDirection(GuiAxis.Y)
                .background(GuiTextures.SLOT_ITEM)
                .width(WIDTH - 24)
                .height(HEIGHT - BODY_Y - 34)
                .pos(CONTENT_LEFT, BODY_Y + 24);

        int index = 0;
        for (String flagId : data.availableFlags) {
            list.addChild(createRow(data, flagId), index++);
        }
        panel.child(list);
        return panel;
    }

    private static Row createRow(FactionFlagSelectGuiData data, String flagId) {
        Row row = new Row();
        row.width(WIDTH - 40);
        row.height(28);
        row.child(new IDrawable.DrawableWidget(new FlagDrawable(flagId)).size(42, 24));
        row.child(new ScrollingTextWidget(IKey.str(displayName(flagId)).color(0xC7CCD1)).width(160).tooltip(t -> t.addLine(flagId)));
        boolean chosen = flagId.equals(data.currentFlagId);
        boolean clickable = data.canChoose && data.currentFlagId.isEmpty();
        row.child(new ButtonWidget<>()
                .width(74)
                .height(18)
                .overlay(IKey.str(chosen ? "Selected" : "Choose").color(chosen || clickable ? 0xFFFFFF : 0x8A8A8A))
                .background(chosen ? GuiTextures.MC_BUTTON_HOVERED : (clickable ? GuiTextures.MC_BUTTON : GuiTextures.MC_BUTTON_DISABLED))
                .hoverBackground(chosen ? GuiTextures.MC_BUTTON_HOVERED : (clickable ? GuiTextures.MC_BUTTON_HOVERED : GuiTextures.MC_BUTTON_DISABLED))
                .onMousePressed(mouseButton -> {
                    if (!clickable) {
                        return false;
                    }
                    PacketChooseFactionFlag packet = new PacketChooseFactionFlag();
                    packet.factionId = data.factionId;
                    packet.flagId = flagId;
                    WarForgeMod.NETWORK.sendToServer(packet);
                    return true;
                }));
        return row;
    }

    private static String displayName(String flagId) {
        String raw = flagId.contains(":") ? flagId.substring(flagId.indexOf(':') + 1) : flagId;
        return raw.replace('_', ' ');
    }

    private static IDrawable sectionBackdrop(int x, int y, int width, int height, int fillColor, int borderColor) {
        return new IDrawable() {
            @Override
            public void draw(GuiContext context, int drawX, int drawY, int drawWidth, int drawHeight, WidgetTheme theme) {
                Gui.drawRect(drawX, drawY, drawX + width, drawY + height, fillColor);
                Gui.drawRect(drawX, drawY, drawX + width, drawY + 1, borderColor);
                Gui.drawRect(drawX, drawY + height - 1, drawX + width, drawY + height, borderColor);
                Gui.drawRect(drawX, drawY, drawX + 1, drawY + height, borderColor);
                Gui.drawRect(drawX + width - 1, drawY, drawX + width, drawY + height, borderColor);
            }
        };
    }

    private static IDrawable colorStripe(int color, int x, int y, int width, int height) {
        return new IDrawable() {
            @Override
            public void draw(GuiContext context, int drawX, int drawY, int drawWidth, int drawHeight, WidgetTheme theme) {
                Gui.drawRect(drawX, drawY, drawX + width, drawY + height, color);
            }
        };
    }
}
