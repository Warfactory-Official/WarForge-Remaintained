package com.flansmod.warforge.api.modularui;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.custom.PlayerInventoryWidget;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.function.Consumer;

public final class WarForgeUiTheme {
    public static final int HEADER_FILL = 0xFF171B1F;
    public static final int HEADER_BORDER = 0xFF0D1013;
    public static final int SECTION_FILL = 0xEE20262B;
    public static final int SECTION_BORDER = 0xEE11161A;
    public static final int INSET_FILL = 0xFF262D33;
    public static final int ROW_FILL = 0xFF232A30;
    public static final int MAP_BACKDROP_FILL = 0xFF161B20;
    public static final int BUTTON_HIGHLIGHT_FILL = 0xFF242B31;
    public static final int BUTTON_DISABLED_FILL = 0xFF1B2024;
    public static final int DANGER_FILL = 0xFF7A2D2D;
    public static final int DANGER_HOVER_FILL = 0xFF944040;

    public static final int TEXT_PRIMARY = 0xFFFFFF;
    public static final int TEXT_SECONDARY = 0xC7CCD1;
    public static final int TEXT_MUTED = 0xB8BDC3;
    public static final int TEXT_DISABLED = 0x8A8A8A;
    public static final int TEXT_SUCCESS = 0x55FF55;
    public static final int TEXT_WARNING = 0xFFE19A;
    public static final int TEXT_DANGER = 0xFFEAEA;
    public static final int TEXT_HINT = 0xD6DBE0;
    public static final int TEXT_LEADER_ACTION = 0xFFCC88;
    public static final int TEXT_DIM = 0xBBBBBB;
    public static final int TEXT_OFFLINE = 0xAAAAAA;
    public static final int TEXT_PENDING = 0xFFAA00;
    public static final int TEXT_BAD = 0xFF5555;
    public static final int UPGRADE_STRIPE = 0x9F7A34;
    public static final int UPGRADE_BEFORE = 0x6CB6FF;
    public static final int UPGRADE_AFTER = 0x8BFFB0;
    public static final int TEXT_TAB_IDLE = 0xCCCCCC;
    public static final int TEXT_NOTE = 0xD5D9DE;

    public static final int STRIPE_DEFAULT = 0x4A4A4A;

    public static final int CONTENT_LEFT = 12;
    public static final int HEADER_HEIGHT = 40;
    public static final int HEADER_Y = 12;
    public static final int SUBTITLE_Y = HEADER_Y + 15;
    public static final int BUTTON_HEIGHT = 18;
    public static final int SECTION_PADDING = 5;
    public static final int STRIPE_WIDTH = 6;
    public static final int CLOSE_SIZE = 10;
    public static final float TITLE_SCALE = 1.15f;

    public static final int INVENTORY_WIDTH = 172;
    public static final int INVENTORY_HEIGHT = 86;
    public static final int INVENTORY_GAP = 8;

    public static final IGuiTexture PANEL_BACKGROUND = ResourceBorderTexture.BORDERED_BACKGROUND;

    private static final Long2ObjectMap<IGuiTexture> BORDERED_FILLS = new Long2ObjectOpenHashMap<>();

    private WarForgeUiTheme() {
    }

    public static IGuiTexture borderedFill(int fill, int border) {
        return BORDERED_FILLS.computeIfAbsent(((long) fill << 32) | (border & 0xFFFFFFFFL),
                key -> new GuiTextureGroup(new ColorRectTexture(fill), new ColorBorderTexture(-1, border)));
    }

    public static IGuiTexture headerBackdrop() {
        return borderedFill(HEADER_FILL, HEADER_BORDER);
    }

    public static IGuiTexture sectionBackdrop() {
        return borderedFill(SECTION_FILL, SECTION_BORDER);
    }

    public static IGuiTexture insetBackdrop() {
        return borderedFill(INSET_FILL, SECTION_BORDER);
    }

    public static IGuiTexture insetBackdrop(int fill) {
        return borderedFill(fill, SECTION_BORDER);
    }

    public static IGuiTexture buttonFill(int fill) {
        return borderedFill(fill, HEADER_BORDER);
    }

    public static IGuiTexture solid(int rgb) {
        return new ColorRectTexture(0xFF000000 | (rgb & 0xFFFFFF));
    }

    public static WidgetGroup group(int x, int y, int width, int height) {
        return new WidgetGroup(x, y, width, height);
    }

    public static WidgetGroup panel(int x, int y, int width, int height) {
        WidgetGroup panel = new WidgetGroup(x, y, width, height);
        panel.setBackground(PANEL_BACKGROUND);
        return panel;
    }

    public static Widget box(int x, int y, int width, int height, IGuiTexture texture) {
        Widget widget = new Widget(x, y, width, height);
        widget.setBackground(texture);
        return widget;
    }

    public static Widget header(int panelWidth) {
        return box(0, 0, panelWidth, HEADER_HEIGHT, headerBackdrop());
    }

    public static Widget section(int x, int y, int width, int height) {
        return box(x, y, width, height, sectionBackdrop());
    }

    public static Widget inset(int x, int y, int width, int height) {
        return box(x, y, width, height, insetBackdrop());
    }

    public static Widget stripe(int rgb, int panelHeight) {
        return box(0, 0, STRIPE_WIDTH, panelHeight, solid(rgb));
    }

    public static Label title(String text) {
        return title(text, TEXT_PRIMARY);
    }

    public static Label title(String text, int rgb) {
        return Label.bold(text, rgb).shadow().scale(TITLE_SCALE).at(CONTENT_LEFT, HEADER_Y);
    }

    public static Label subtitle(String text, int rgb, boolean bold) {
        return (bold ? Label.bold(text, rgb) : Label.text(text, rgb)).at(CONTENT_LEFT, SUBTITLE_Y);
    }

    public static Label text(String value, int rgb) {
        return Label.text(value, rgb);
    }

    public static PlayerInventoryWidget inventoryBox(int x, int y) {
        PlayerInventoryWidget inventory = new PlayerInventoryWidget();
        inventory.setSelfPosition(x, y);
        inventory.setBackground(PANEL_BACKGROUND);
        return inventory;
    }

    public static PlayerInventoryWidget inventorySlots(int x, int y) {
        PlayerInventoryWidget inventory = new PlayerInventoryWidget();
        inventory.setSelfPosition(x - 5, y - 5);
        return inventory;
    }

    public static ButtonWidget closeButton(int panelWidth, Consumer<ClickData> onPress) {
        ButtonWidget button = new ButtonWidget(panelWidth - 18, 8, CLOSE_SIZE, CLOSE_SIZE,
                new GuiTextureGroup(ResourceBorderTexture.VANILLA_BUTTON_NORMAL, CrossTexture.INSTANCE), onPress);
        button.setHoverTexture(new GuiTextureGroup(ResourceBorderTexture.VANILLA_BUTTON_SELECTED, CrossTexture.INSTANCE));
        button.setDrawBackgroundWhenHover(false);
        return button;
    }

    public static ButtonWidget actionButton(String label, int width, Consumer<ClickData> onPress) {
        return actionButton(label, width, true, onPress);
    }

    public static ButtonWidget actionButton(String label, int width, boolean enabled, Consumer<ClickData> onPress) {
        if (!enabled) {
            return button(label, width, BUTTON_HEIGHT, BUTTON_DISABLED_FILL, 0, TEXT_DISABLED, null);
        }
        return button(label, width, BUTTON_HEIGHT, HEADER_FILL, BUTTON_HIGHLIGHT_FILL, TEXT_PRIMARY, onPress);
    }

    public static ButtonWidget tabButton(String label, int width, boolean selected, Consumer<ClickData> onPress) {
        return button(label, width, BUTTON_HEIGHT, selected ? BUTTON_HIGHLIGHT_FILL : HEADER_FILL, BUTTON_HIGHLIGHT_FILL,
                selected ? TEXT_PRIMARY : TEXT_TAB_IDLE, onPress);
    }

    public static ButtonWidget dangerButton(String label, int width, Consumer<ClickData> onPress) {
        return smallDangerButton(label, width, BUTTON_HEIGHT, onPress);
    }

    public static ButtonWidget smallDangerButton(String label, int width, int height, Consumer<ClickData> onPress) {
        return button(label, width, height, DANGER_FILL, DANGER_HOVER_FILL, TEXT_DANGER, onPress);
    }

    public static ButtonWidget vanillaButton(String label, int width, int height, Consumer<ClickData> onPress) {
        TextTexture text = buttonText(label, TEXT_PRIMARY);
        ButtonWidget button = new ButtonWidget(0, 0, width, height,
                new GuiTextureGroup(ResourceBorderTexture.VANILLA_BUTTON_NORMAL, text), onPress);
        button.setHoverTexture(new GuiTextureGroup(ResourceBorderTexture.VANILLA_BUTTON_SELECTED, text));
        button.setDrawBackgroundWhenHover(false);
        return button;
    }

    private static ButtonWidget button(String label, int width, int height, int fill, int hoverFill, int textColor,
                                       Consumer<ClickData> onPress) {
        TextTexture text = buttonText(label, textColor);
        ButtonWidget button = new ButtonWidget(0, 0, width, height, new GuiTextureGroup(buttonFill(fill), text), onPress);
        if (onPress != null) {
            button.setHoverTexture(new GuiTextureGroup(buttonFill(hoverFill), text));
            button.setDrawBackgroundWhenHover(false);
        }
        return button;
    }

    private static TextTexture buttonText(String label, int rgb) {
        return new TextTexture(label, 0xFF000000 | (rgb & 0xFFFFFF)).setDropShadow(true);
    }

    public static <T extends Widget> T at(T widget, int x, int y) {
        widget.setSelfPosition(x, y);
        return widget;
    }
}
