package com.flansmod.warforge.api.modularui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;

/**
 * A vertically scrolling list with the inset backdrop (the ModularUI ListWidget of the 1.20.1 menus). Add rows with
 * {@link #addRow}, then call {@link #finish()}; the scroll bar only appears when the rows overflow.
 * <p>
 * LDLib hides the children that are scrolled out of view. On the server the list never scrolls, so there every
 * child stays visible: a hidden slot is disabled, and the server must accept the slots the client scrolled to.
 */
public class ScrollArea extends DraggableScrollableWidgetGroup {
    private static final int BAR_WIDTH = 4;

    private int cursor;

    public ScrollArea(int x, int y, int width, int height) {
        super(x, y, width, height);
        setBackground(WarForgeUiTheme.insetBackdrop());
    }

    /** Adds a row at x under the previous rows, with the given space below it. */
    public ScrollArea addRow(Widget row, int x, int marginTop, int marginBottom) {
        row.setSelfPosition(x, cursor + marginTop);
        addWidget(row);
        cursor += marginTop + row.getSizeHeight() + marginBottom;
        return this;
    }

    /** Adds a widget at a fixed position (for grids). */
    public ScrollArea addAt(Widget widget, int x, int y) {
        widget.setSelfPosition(x, y);
        addWidget(widget);
        cursor = Math.max(cursor, y + widget.getSizeHeight());
        return this;
    }

    /** Shows the scroll bar if the content is taller than the list. */
    public ScrollArea finish() {
        if (cursor > getSizeHeight()) {
            setYScrollBarWidth(BAR_WIDTH);
            setYBarStyle(new ColorRectTexture(0x40000000), new ColorRectTexture(0xFF5A646E));
        }
        keepVisibleOnServer();
        return this;
    }

    @Override
    public WidgetGroup addWidget(int index, Widget widget) {
        WidgetGroup result = super.addWidget(index, widget);
        keepVisibleOnServer();
        return result;
    }

    @Override
    public void computeMax() {
        super.computeMax();
        keepVisibleOnServer();
    }

    @Override
    public void setSize(Size size) {
        super.setSize(size);
        keepVisibleOnServer();
    }

    private void keepVisibleOnServer() {
        boolean remote = gui != null && gui.holder != null ? gui.holder.isRemote() : LDLib.isRemote();
        if (!remote && widgets != null) {
            for (Widget widget : widgets) {
                widget.setVisible(true);
            }
        }
    }
}
