package com.flansmod.warforge.api.modularui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * WarForge menus on LDLib2 = ModularUI menus of 1.20.1 (ModularGuiStyle). Layout parity with ModularUI:
 * <ul>
 * <li>Flow: children on main axis, centred on cross axis ({@link #section}, {@link #flow}).</li>
 * <li>Text widget: as wide as its text, wrapped at parent width ({@link #fit}, {@link #wrap}).</li>
 * <li>{@code Text.BOLD} / {@code style(BOLD)} never rendered there => plain text; only inline formatting is bold.</li>
 * </ul>
 */
public final class WarForgeUiTheme {
    // Colours of ModularGuiStyle (1.20.1)
    public static final int HEADER_FILL = 0xFF171B1F;
    public static final int HEADER_BORDER = 0xFF0D1013;
    public static final int SECTION_FILL = 0xEE20262B;
    public static final int SECTION_BORDER = 0xEE11161A;
    public static final int INSET_FILL = 0xFF262D33;
    public static final int ROW_FILL = 0xFF232A30;
    public static final int BUTTON_HIGHLIGHT_FILL = 0xFF242B31;
    public static final int BUTTON_DISABLED_FILL = 0xFF1B2024;
    public static final int MAP_BACKDROP_FILL = 0xFF161B20;
    public static final int TEXT_PRIMARY = 0xFFFFFF;
    public static final int TEXT_SECONDARY = 0xC7CCD1;
    public static final int TEXT_MUTED = 0xB8BDC3;
    public static final int TEXT_DISABLED = 0x8A8A8A;
    public static final int TEXT_SUCCESS = 0x55FF55;
    public static final int TEXT_WARNING = 0xFFE19A;
    public static final int TEXT_DANGER = 0xFFEAEA;
    public static final int TEXT_INFO = 0xD5D9DE;
    public static final int DANGER_FILL = 0xFF7A2D2D;
    public static final int DANGER_HOVER_FILL = 0xFF944040;
    /** Stripe colour when there is no faction. */
    public static final int NO_FACTION_STRIPE = 0x4A4A4A;

    // Common layout of the 1.20.1 panels
    public static final int CONTENT_LEFT = 12;
    public static final int HEADER_HEIGHT = 40;
    public static final int HEADER_Y = 12;
    public static final int SUBTITLE_Y = 27;
    public static final int BODY_Y = 54;
    public static final int INVENTORY_GAP = 8;
    /** {@link #inventoryPanel} height. */
    public static final int INVENTORY_BOX_HEIGHT = 86;
    public static final int BUTTON_HEIGHT = 18;
    public static final float TITLE_FONT_SIZE = 9f * 1.15f;

    private WarForgeUiTheme() {
    }

    // ---------------------------------------------------------------- textures

    public static IGuiTexture fill(int argb) {
        return new ColorRectTexture(argb);
    }

    /** A filled rectangle with a 1 pixel border inside its bounds. */
    public static IGuiTexture borderedFill(int fill, int border) {
        return IGuiTexture.group(new ColorRectTexture(fill), new ColorBorderTexture(-1, border));
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

    // ---------------------------------------------------------------- layout

    /**
     * The root of a screen: its panels stacked top to bottom and centred (the panel and the player inventory
     * below it are centred on the screen as one group, like the 1.20.1 menus).
     */
    public static UIElement screenRoot() {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER));
        return root;
    }

    /** A fixed size panel with the vanilla grey background. Its parts are added with {@link #place}. */
    public static UIElement panel(int width, int height) {
        return panel(width, height, true);
    }

    /**
     * A fixed size panel. Block UIs are built on both sides: pass client = false on the server, where the
     * background painter is replaced by an empty element (same element tree, no client rendering classes).
     */
    public static UIElement panel(int width, int height, boolean client) {
        UIElement panel = new UIElement();
        panel.layout(l -> l.width(width).height(height));
        place(panel, backdrop(client), 0, 0, width, height);
        return panel;
    }

    private static UIElement backdrop(boolean client) {
        return client ? new VanillaPanelElement() : new UIElement();
    }

    /** Positions child at (x, y) inside parent with the given size, and adds it. */
    public static <T extends UIElement> T place(UIElement parent, T child, float x, float y, float width, float height) {
        child.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(width).height(height));
        parent.addChild(child);
        return child;
    }

    /** Positions child at (x, y) inside parent keeping its own size, and adds it. */
    public static <T extends UIElement> T place(UIElement parent, T child, float x, float y) {
        child.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y));
        parent.addChild(child);
        return child;
    }

    /** A plain element of a fixed size (spacer or container). */
    public static UIElement box(int width, int height) {
        UIElement box = new UIElement();
        box.layout(l -> l.width(width).height(height).flexShrink(0));
        return box;
    }

    /** A plain element with a background. */
    public static UIElement box(int width, int height, IGuiTexture background) {
        UIElement box = box(width, height);
        box.style(s -> s.background(background));
        return box;
    }

    /** The dark header strip across the top of a panel. */
    public static UIElement headerBackdrop(UIElement panel, int width) {
        return place(panel, box(width, HEADER_HEIGHT, headerBackdrop()), 0, 0, width, HEADER_HEIGHT);
    }

    /** The faction colour stripe down the left of a panel. */
    public static UIElement colorStripe(UIElement panel, int color, int height) {
        return place(panel, box(6, height, fill(0xFF000000 | (color & 0xFFFFFF))), 0, 0, 6, height);
    }

    /** ModularUI Flow: main axis start, cross axis centre. */
    public static UIElement flow(FlexDirection direction) {
        UIElement flow = new UIElement();
        flow.layout(l -> l.flexDirection(direction).alignItems(AlignItems.CENTER).flexShrink(0));
        return flow;
    }

    /** A dark section: column flow inside a 5 pixel padding. */
    public static UIElement section(int width, int height) {
        UIElement section = flow(FlexDirection.COLUMN);
        section.layout(l -> l.width(width).height(height).paddingAll(5));
        section.style(s -> s.background(sectionBackdrop()));
        return section;
    }

    /** Flow child at a fixed cross-axis distance from the parent's end edge (ModularUI {@code right(n)}). */
    public static <T extends UIElement> T fromEnd(T child, float distance, float parentPadding) {
        child.layout(l -> l.alignSelf(AlignItems.END).marginRight(distance - parentPadding));
        return child;
    }

    /** A section placed at (x, y) in the panel. */
    public static UIElement section(UIElement panel, int x, int y, int width, int height) {
        return place(panel, section(width, height), x, y, width, height);
    }

    /** A horizontal row of the given size, children centred vertically. */
    public static UIElement row(int width, int height) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).width(width).height(height).flexShrink(0));
        return row;
    }

    /** A row of an inset list (dark bordered, 3 pixel padding, 2 pixel gap below). */
    public static UIElement listRow(int width, int height) {
        UIElement row = row(width, height);
        row.layout(l -> l.paddingAll(3).marginBottom(2));
        row.style(s -> s.background(insetBackdrop(ROW_FILL)));
        return row;
    }

    /** A vertical scroll list with the inset background. Add rows with {@link ScrollerView#addScrollViewChild}. */
    public static ScrollerView insetList(int width, int height) {
        return insetList(width, height, insetBackdrop());
    }

    public static ScrollerView insetList(int width, int height, IGuiTexture background) {
        ScrollerView list = new ScrollerView();
        list.layout(l -> l.width(width).height(height).flexShrink(0));
        list.scrollerStyle(s -> s.horizontalScrollDisplay(ScrollDisplay.NEVER).verticalScrollDisplay(ScrollDisplay.AUTO));
        list.viewPort(v -> {
            v.layout(l -> l.paddingAll(0));
            v.style(s -> s.background(background));
        });
        // ModularUI ListWidget: rows centred on the cross axis.
        list.viewContainer(c -> c.layout(l -> l.alignItems(AlignItems.CENTER).minWidthPercent(100)));
        return list;
    }

    /**
     * The vanilla slot sprite, as the 1.20.1 menus draw their slots. LDLib2's own slot sprite is nine-sliced, and
     * LDLib2 26 draws it with a thicker, darker rim than LDLib2 for 1.21.1; this 18x18 sprite looks the same on both.
     */
    private static final IGuiTexture VANILLA_SLOT = SpriteTexture.of("minecraft:textures/gui/sprites/container/slot.png");

    /** Gives every item slot in the element (and its children) the vanilla slot background. */
    public static <T extends UIElement> T vanillaSlots(T element) {
        if (element instanceof ItemSlot slot) {
            slot.style(s -> s.backgroundTexture(VANILLA_SLOT));
        }
        for (UIElement child : element.getChildren()) {
            vanillaSlots(child);
        }
        return element;
    }

    /** Player inventory in a vanilla grey box (5 pixel padding), gap below the previous root child. See {@link #panel(int, int, boolean)} for client. */
    public static UIElement inventoryPanel(boolean client, int gap) {
        InventorySlots inventory = vanillaSlots(new InventorySlots());
        UIElement box = new UIElement();
        box.layout(l -> l.paddingAll(5).marginTop(gap));
        UIElement backdrop = backdrop(client);
        backdrop.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).widthPercent(100).heightPercent(100));
        box.addChild(backdrop);
        box.addChild(inventory);
        return box;
    }

    // ---------------------------------------------------------------- text

    public static Component literal(String value, int rgb, boolean bold) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF));
        if (bold) {
            style = style.withBold(true);
        }
        return Component.literal(value).withStyle(style);
    }

    /** ModularUI text widget height: lines * 9 - 1 (8 for one line); lines 9 apart. */
    public static Label label(Component value) {
        Label label = new Label();
        label.setValue(value);
        label.textStyle(t -> t.textShadow(false).lineSpacing(0));
        label.layout(l -> l.height(8));
        return label;
    }

    public static Label text(String value, int rgb) {
        return label(literal(value, rgb, false));
    }

    public static Label boldText(String value, int rgb) {
        return label(literal(value, rgb, true));
    }

    public static Label shadowText(String value, int rgb, boolean bold) {
        Label label = label(literal(value, rgb, bold));
        label.textStyle(t -> t.textShadow(true));
        return label;
    }

    /** Panel header title: shadow, 1.15 scale. */
    public static Label title(String value, int rgb) {
        Label label = fit(shadowText(value, rgb, false));
        label.textStyle(t -> t.fontSize(TITLE_FONT_SIZE));
        label.layout(l -> l.height(10));
        return label;
    }

    public static Label title(UIElement panel, String value, int rgb) {
        return place(panel, title(value, rgb), CONTENT_LEFT, HEADER_Y);
    }

    public static Label subtitle(UIElement panel, Label label) {
        return place(panel, fit(label), CONTENT_LEFT, SUBTITLE_Y);
    }

    /** As wide as its text (one line). */
    public static <T extends Label> T fit(T label) {
        label.textStyle(t -> t.adaptiveWidth(true));
        label.layout(l -> l.flexShrink(0));
        return label;
    }

    /**
     * As wide as its widest line, wrapped at maxWidth (ModularUI wraps at the parent's width). Server-side trees
     * (client = false) are never drawn, so they skip the font.
     */
    public static Label wrap(Label label, int maxWidth, boolean client) {
        int[] size = client ? ClientText.wrappedSize(label.getText(), maxWidth) : new int[]{maxWidth, 8};
        label.textStyle(t -> t.textWrap(TextWrap.WRAP));
        label.layout(l -> l.width(size[0]).height(size[1]).flexShrink(0));
        return label;
    }

    public static Label wrap(Label label, int maxWidth) {
        return wrap(label, maxWidth, true);
    }

    /** Text that scrolls when hovered if it is wider than its box (ScrollingTextWidget of 1.20.1). */
    public static Label scrollingText(Component value, int width) {
        Label label = label(value);
        label.layout(l -> l.width(width).flexShrink(0));
        label.textStyle(t -> t.textWrap(TextWrap.HOVER_ROLL));
        return label;
    }

    /** Gives a flow child a fixed width. */
    public static <T extends UIElement> T width(T element, int width) {
        element.layout(l -> l.width(width).flexShrink(0));
        return element;
    }

    // ---------------------------------------------------------------- tooltips

    public static <T extends UIElement> T tooltip(T element, String... lines) {
        Component[] components = new Component[lines.length];
        for (int i = 0; i < lines.length; i++) {
            components[i] = Component.literal(lines[i]);
        }
        return tooltip(element, components);
    }

    public static <T extends UIElement> T tooltip(T element, Component... lines) {
        element.style(s -> s.tooltips(lines));
        return element;
    }

    /** A tooltip computed each time it is shown. */
    public static <T extends UIElement> T dynamicTooltip(T element, Supplier<List<Component>> lines) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, (UIEventListener) event -> {
            List<Component> list = lines.get();
            if (list.isEmpty()) {
                return;
            }
            event.hoverTooltips = (event.hoverTooltips == null ? HoverTooltips.empty() : event.hoverTooltips)
                    .append(list.toArray(new Component[0]));
        });
        return element;
    }

    // ---------------------------------------------------------------- buttons

    private static Button button(String label, int width, int height, int textColor) {
        Button button = new Button();
        button.setText(Component.literal(label));
        // The TEXT_* colours are RGB. Before 1.21.6 text drew them opaque; 26.x reads a zero alpha as invisible.
        button.textStyle(t -> t.textColor(0xFF000000 | textColor).textShadow(true));
        button.layout(l -> l.width(width).height(height).flexShrink(0));
        return button;
    }

    private static Button skin(Button button, IGuiTexture base, IGuiTexture hover) {
        button.buttonStyle(s -> s.baseTexture(base).hoverTexture(hover).pressedTexture(hover));
        return button;
    }

    /** A dark action button, 18 pixels high (actionButton of 1.20.1). A disabled one is greyed and does nothing. */
    public static Button actionButton(String label, int width, boolean enabled, Runnable action) {
        Button button = actionButton(label, width, enabled);
        if (enabled && action != null) {
            button.setOnClick(event -> action.run());
        }
        return button;
    }

    public static Button actionButton(String label, int width, Runnable action) {
        return actionButton(label, width, true, action);
    }

    /** A dark action button without a client action (for block UI buttons with a server action). */
    public static Button actionButton(String label, int width, boolean enabled) {
        Button button = button(label, width, BUTTON_HEIGHT, enabled ? TEXT_PRIMARY : TEXT_DISABLED);
        if (enabled) {
            return skin(button, borderedFill(HEADER_FILL, HEADER_BORDER), borderedFill(BUTTON_HIGHLIGHT_FILL, HEADER_BORDER));
        }
        IGuiTexture disabled = borderedFill(BUTTON_DISABLED_FILL, HEADER_BORDER);
        return skin(button, disabled, disabled);
    }

    /** A tab button; the selected one is highlighted. */
    public static Button tabButton(String label, int width, boolean selected, Runnable action) {
        Button button = button(label, width, BUTTON_HEIGHT, selected ? TEXT_PRIMARY : 0xCCCCCC);
        skin(button, borderedFill(selected ? BUTTON_HIGHLIGHT_FILL : HEADER_FILL, HEADER_BORDER),
                borderedFill(BUTTON_HIGHLIGHT_FILL, HEADER_BORDER));
        if (action != null) {
            button.setOnClick(event -> action.run());
        }
        return button;
    }

    /** A red button (dangerButton of 1.20.1). */
    public static Button dangerButton(String label, int width, int height, Runnable action) {
        Button button = button(label, width, height, TEXT_DANGER);
        skin(button, borderedFill(DANGER_FILL, HEADER_BORDER), borderedFill(DANGER_HOVER_FILL, HEADER_BORDER));
        if (height < 14) {
            button.layout(l -> l.paddingAll(0));
            button.text.layout(l -> l.marginHorizontal(0));
            button.textStyle(t -> t.fontSize(Math.max(6f, height - 1f)));
        }
        if (action != null) {
            button.setOnClick(event -> action.run());
        }
        return button;
    }

    public static Button dangerButton(String label, int width, Runnable action) {
        return dangerButton(label, width, BUTTON_HEIGHT, action);
    }

    /** A grey vanilla style button (MC_BUTTON of 1.20.1). */
    public static Button vanillaButton(String label, int width, int height) {
        Button button = button(label, width, height, TEXT_PRIMARY);
        return skin(button,
                IGuiTexture.group(new ColorRectTexture(0xFF6F6F6F), new ColorBorderTexture(-1, 0xFF000000)),
                IGuiTexture.group(new ColorRectTexture(0xFF8C8C8C), new ColorBorderTexture(-1, 0xFFFFFFFF)));
    }

    /** Panel close button: ModularUI CROSS_TINY glyph, no backdrop, 10x10 at (panelWidth - 18, 8). */
    public static Button closeButton(UIElement panel, int panelWidth, boolean client) {
        Button button = new Button();
        button.setText(Component.empty());
        button.buttonStyle(s -> s.baseTexture(IGuiTexture.EMPTY).hoverTexture(IGuiTexture.EMPTY).pressedTexture(IGuiTexture.EMPTY));
        button.layout(l -> l.paddingAll(0));
        if (client) {
            UIElement cross = new CrossElement();
            cross.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).width(10).height(10));
            button.addChild(cross);
        }
        return place(panel, button, panelWidth - 18, 8, 10, 10);
    }

    public static Button closeButton(UIElement panel, int panelWidth) {
        return closeButton(panel, panelWidth, true);
    }

    // ---------------------------------------------------------------- small parts

    /** A solid colour square (faction swatch). */
    public static UIElement swatch(int color, int size) {
        return box(size, size, fill(0xFF000000 | (color & 0xFFFFFF)));
    }

    /** An item icon (no slot, not interactive). */
    public static UIElement itemIcon(ItemStack stack, int size) {
        return box(size, size, new ItemStackTexture(stack));
    }
}
