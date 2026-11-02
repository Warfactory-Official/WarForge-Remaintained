package com.flansmod.warforge.api.modularui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;

/** Server: every child stays visible (hidden slot is disabled; server must accept scrolled-to slots). */
public class ScrollArea extends DraggableScrollableWidgetGroup {
    private static final int BAR_WIDTH = 4;

    private int cursor;

    public ScrollArea(int x, int y, int width, int height) {
        super(x, y, width, height);
        setBackground(WarForgeUiTheme.insetBackdrop());
    }

    public ScrollArea addRow(Widget row, int x, int marginTop, int marginBottom) {
        row.setSelfPosition(x, cursor + marginTop);
        addWidget(row);
        cursor += marginTop + row.getSizeHeight() + marginBottom;
        return this;
    }

    public ScrollArea addAt(Widget widget, int x, int y) {
        widget.setSelfPosition(x, y);
        addWidget(widget);
        cursor = Math.max(cursor, y + widget.getSizeHeight());
        return this;
    }

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
        if (!isRemoteGui()) {
            widget.setVisible(true);
        }
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

    private boolean isRemoteGui() {
        return gui != null && gui.holder != null ? gui.holder.isRemote() : LDLib.isRemote();
    }

    private void keepVisibleOnServer() {
        if (!isRemoteGui() && widgets != null) {
            for (Widget widget : widgets) {
                widget.setVisible(true);
            }
        }
    }
}
