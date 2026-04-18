package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import net.minecraft.client.gui.Gui;

public final class ModularGuiStyle {
    public static final int HEADER_FILL = 0xFF171B1F;
    public static final int HEADER_BORDER = 0xFF0D1013;
    public static final int SECTION_FILL = 0xEE20262B;
    public static final int SECTION_BORDER = 0xEE11161A;
    public static final int INSET_FILL = 0xFF262D33;
    public static final int BUTTON_HIGHLIGHT_FILL = 0xFF242B31;
    public static final int TEXT_PRIMARY = 0xFFFFFF;
    public static final int TEXT_SECONDARY = 0xC7CCD1;
    public static final int TEXT_MUTED = 0xB8BDC3;
    public static final int TEXT_DISABLED = 0x8A8A8A;
    public static final int TEXT_SUCCESS = 0x55FF55;
    public static final int TEXT_WARNING = 0xFFE19A;
    public static final int TEXT_DANGER = 0xFFEAEA;
    public static final int DANGER_FILL = 0xFF7A2D2D;
    public static final int DANGER_HOVER_FILL = 0xFF944040;

    private ModularGuiStyle() {
    }

    public static IDrawable headerBackdrop() {
        return borderedFill(HEADER_FILL, HEADER_BORDER);
    }

    public static IDrawable sectionBackdrop() {
        return borderedFill(SECTION_FILL, SECTION_BORDER);
    }

    public static IDrawable insetBackdrop() {
        return borderedFill(INSET_FILL, SECTION_BORDER);
    }

    public static IDrawable insetBackdrop(int color) {
        return borderedFill(color, SECTION_BORDER);
    }

    public static IDrawable colorStripe(int color) {
        return (context, drawX, drawY, drawWidth, drawHeight, theme) ->
                Gui.drawRect(drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0xFF000000 | (color & 0x00FFFFFF));
    }

    public static Flow section(int width, int height) {
        return new Flow(GuiAxis.Y)
                .background(sectionBackdrop())
                .size(width, height)
                .padding(5)
                .margin(5);
    }

    public static ParentWidget<?> playerInventoryPanel(int top) {
        return new ParentWidget<>()
                .name("player_inventory_panel")
                .child(SlotGroupWidget.playerInventory(false))
                .background(GuiTextures.MC_BACKGROUND)
                .coverChildren()
                .margin(5)
                .padding(5)
                .horizontalCenter()
                .top(top);
    }

    public static ButtonWidget<?> panelCloseButton(int panelWidth) {
        return ButtonWidget.panelCloseButton().name("panel_close_button").pos(panelWidth - 18, 8).size(10);
    }

    public static ButtonWidget<?> actionButton(String label, int width, Runnable action) {
        return actionButton(label, width, true, action);
    }

    public static ButtonWidget<?> actionButton(String label, int width, boolean enabled, Runnable action) {
        return new ButtonWidget<>()
                .name(debugName("button", label))
                .width(width)
                .height(18)
                .overlay(IKey.str(label).color(enabled ? TEXT_PRIMARY : TEXT_DISABLED))
                .background(enabled ? GuiTextures.MC_BUTTON : GuiTextures.MC_BUTTON_DISABLED, buttonTinted(enabled ? HEADER_FILL : 0xFF1B2024))
                .hoverBackground(enabled ? GuiTextures.MC_BUTTON : GuiTextures.MC_BUTTON_DISABLED, buttonTinted(enabled ? BUTTON_HIGHLIGHT_FILL : 0xFF1B2024))
                .onMousePressed(mouseButton -> {
                    if (!enabled) {
                        return false;
                    }
                    action.run();
                    return true;
                });
    }

    public static ButtonWidget<?> tabButton(String label, int width, boolean selected, Runnable action) {
        return new ButtonWidget<>()
                .name(debugName("tab", label))
                .width(width)
                .height(18)
                .overlay(IKey.str(label).color(selected ? TEXT_PRIMARY : 0xCCCCCC))
                .background(GuiTextures.MC_BUTTON, buttonTinted(selected ? BUTTON_HIGHLIGHT_FILL : HEADER_FILL))
                .hoverBackground(GuiTextures.MC_BUTTON, buttonTinted(BUTTON_HIGHLIGHT_FILL))
                .onMousePressed(mouseButton -> {
                    if (!selected) {
                        action.run();
                    }
                    return true;
                });
    }

    public static ButtonWidget<?> dangerButton(String label, int width, Runnable action) {
        return new ButtonWidget<>()
                .name(debugName("danger_button", label))
                .width(width)
                .height(18)
                .overlay(IKey.str(label).color(TEXT_DANGER))
                .background(buttonFill(DANGER_FILL))
                .hoverBackground(buttonFill(DANGER_HOVER_FILL))
                .onMousePressed(mouseButton -> {
                    action.run();
                    return true;
                });
    }

    public static ButtonWidget<?> smallDangerButton(String label, int width, int height, Runnable action) {
        return new ButtonWidget<>()
                .name(debugName("danger_button", label))
                .width(width)
                .height(height)
                .overlay(IKey.str(label).color(TEXT_DANGER))
                .background(buttonFill(DANGER_FILL))
                .hoverBackground(buttonFill(DANGER_HOVER_FILL))
                .onMousePressed(mouseButton -> {
                    action.run();
                    return true;
                });
    }

    public static IDrawable buttonFill(int color) {
        return borderedFill(color, HEADER_BORDER);
    }

    public static IDrawable buttonTinted(int color) {
        return borderedFill(color, HEADER_BORDER);
    }

    public static String debugName(String prefix, String text) {
        return prefix + "_" + debugToken(text);
    }

    private static IDrawable borderedFill(int fillColor, int borderColor) {
        return (context, x, y, width, height, theme) -> {
            Gui.drawRect(x, y, x + width, y + height, fillColor);
            Gui.drawRect(x, y, x + width, y + 1, borderColor);
            Gui.drawRect(x, y + height - 1, x + width, y + height, borderColor);
            Gui.drawRect(x, y, x + 1, y + height, borderColor);
            Gui.drawRect(x + width - 1, y, x + width, y + height, borderColor);
        };
    }

    private static String debugToken(String text) {
        String normalized = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized.isEmpty() ? "unnamed" : normalized;
    }
}
