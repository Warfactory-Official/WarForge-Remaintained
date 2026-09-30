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

import java.util.function.Consumer;

/**
 * The WarForge look for LDLib1 widgets, taken from the ModularUI menus of the 1.20.1 branch (ModularGuiStyle):
 * a vanilla-style panel with a dark header strip (40px), a faction colour stripe on the left (6px), dark bordered
 * sections, inset list backdrops, flat bordered buttons and the small close button in the top right corner.
 * <p>
 * The menus are laid out with absolute positions like the 1.20.1 panels (LDLib1 positions widgets absolutely too).
 */
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
    public static final int TEXT_TAB_IDLE = 0xCCCCCC;
    public static final int TEXT_NOTE = 0xD5D9DE;

    /** Stripe colour when there is no faction. */
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

    /** The player inventory box (9x4 slots with 5px padding). */
    public static final int INVENTORY_WIDTH = 172;
    public static final int INVENTORY_HEIGHT = 86;
    public static final int INVENTORY_GAP = 8;

    /** The vanilla-style panel background (ModularUI's default panel background). */
    public static final IGuiTexture PANEL_BACKGROUND = ResourceBorderTexture.BORDERED_BACKGROUND;

    private WarForgeUiTheme() {
    }

    // ---------------------------------------------------------------- textures

    /** A filled rectangle with a 1px border drawn inside it. */
    public static IGuiTexture borderedFill(int fill, int border) {
        return new GuiTextureGroup(new ColorRectTexture(fill), new ColorBorderTexture(-1, border));
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

    // ---------------------------------------------------------------- panel parts

    /** An empty group of the given size (no background), e.g. the root holding a panel and the inventory box. */
    public static WidgetGroup group(int x, int y, int width, int height) {
        return new WidgetGroup(x, y, width, height);
    }

    /** The main panel with the vanilla-style background. */
    public static WidgetGroup panel(int x, int y, int width, int height) {
        WidgetGroup panel = new WidgetGroup(x, y, width, height);
        panel.setBackground(PANEL_BACKGROUND);
        return panel;
    }

    /** A plain widget that only draws the texture. */
    public static Widget box(int x, int y, int width, int height, IGuiTexture texture) {
        Widget widget = new Widget(x, y, width, height);
        widget.setBackground(texture);
        return widget;
    }

    /** The dark header strip across the top of a panel. */
    public static Widget header(int panelWidth) {
        return box(0, 0, panelWidth, HEADER_HEIGHT, headerBackdrop());
    }

    public static Widget section(int x, int y, int width, int height) {
        return box(x, y, width, height, sectionBackdrop());
    }

    public static Widget inset(int x, int y, int width, int height) {
        return box(x, y, width, height, insetBackdrop());
    }

    /** The faction colour stripe on the left edge of a panel. */
    public static Widget stripe(int rgb, int panelHeight) {
        return box(0, 0, STRIPE_WIDTH, panelHeight, solid(rgb));
    }

    /** The big bold title of a panel header (at 12,12, scale 1.15, shadow). */
    public static Label title(String text) {
        return title(text, TEXT_PRIMARY);
    }

    public static Label title(String text, int rgb) {
        return Label.bold(text, rgb).shadow().scale(TITLE_SCALE).at(CONTENT_LEFT, HEADER_Y);
    }

    /** The line under the title (at 12,27). */
    public static Label subtitle(String text, int rgb, boolean bold) {
        return (bold ? Label.bold(text, rgb) : Label.text(text, rgb)).at(CONTENT_LEFT, SUBTITLE_Y);
    }

    public static Label text(String value, int rgb) {
        return Label.text(value, rgb);
    }

    public static Label boldText(String value, int rgb) {
        return Label.bold(value, rgb);
    }

    /** The player's inventory (synced slots) in a vanilla box of 172x86 at x, y. */
    public static PlayerInventoryWidget inventoryBox(int x, int y) {
        PlayerInventoryWidget inventory = new PlayerInventoryWidget();
        inventory.setSelfPosition(x, y);
        inventory.setBackground(PANEL_BACKGROUND);
        return inventory;
    }

    /** The player's inventory slots without a box; x, y is the first slot's corner. */
    public static PlayerInventoryWidget inventorySlots(int x, int y) {
        PlayerInventoryWidget inventory = new PlayerInventoryWidget();
        inventory.setSelfPosition(x - 5, y - 5);
        return inventory;
    }

    // ---------------------------------------------------------------- buttons

    /** The small close button in the top right corner of a panel (10x10 at width-18, 8). */
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

    /** A flat header-coloured button (height 18); a disabled one is greyed out and ignores clicks. */
    public static ButtonWidget actionButton(String label, int width, boolean enabled, Consumer<ClickData> onPress) {
        if (!enabled) {
            return button(label, width, BUTTON_HEIGHT, BUTTON_DISABLED_FILL, 0, TEXT_DISABLED, null);
        }
        return button(label, width, BUTTON_HEIGHT, HEADER_FILL, BUTTON_HIGHLIGHT_FILL, TEXT_PRIMARY, onPress);
    }

    /** A page tab: the selected one is highlighted. */
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

    /** A vanilla (grey textured) button, as the basic claim menu uses. */
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

    /** Positions a widget (relative to its parent) and returns it. */
    public static <T extends Widget> T at(T widget, int x, int y) {
        widget.setSelfPosition(x, y);
        return widget;
    }
}
