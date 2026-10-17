package com.flansmod.warforge.client.util;

import com.flansmod.warforge.api.modularui.WFUiElement;
import com.flansmod.warforge.client.render.UiPainters;

import com.flansmod.warforge.client.render.Gui2D;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.Color;

@OnlyIn(Dist.CLIENT)
public class ColorPickerElement extends UIElement {
    private static final int BAR_HEIGHT = 12;
    private static final int BAR_GAP = 3;

    private final float[] hsb = new float[3];

    public ColorPickerElement(int rgb) {
        this(rgb, 40, 8);
    }

    /** A picker with a swatch of the given width, gap pixels to the right of the three bars. */
    public ColorPickerElement(int rgb, int swatchWidth, int gap) {
        Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
        layout(l -> l.flexDirection(FlexDirection.ROW).gapColumn(gap).widthStretch());

        UIElement bars = new UIElement();
        bars.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1));
        for (int channel = 0; channel < 3; channel++) {
            Bar bar = new Bar(channel);
            if (channel < 2) {
                bar.layout(l -> l.marginBottom(BAR_GAP));
            }
            bars.addChild(bar);
        }
        addChild(bars);

        Swatch swatch = new Swatch();
        swatch.layout(l -> l.width(swatchWidth).height(BAR_HEIGHT * 3 + BAR_GAP * 2).flexShrink(0));
        addChild(swatch);
    }

    public int getRGB() {
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }


    private final class Bar extends WFUiElement {
        private final int channel;

        private Bar(int channel) {
            this.channel = channel;
            layout(l -> l.height(BAR_HEIGHT).widthStretch());
            addEventListener(UIEvents.MOUSE_DOWN, (UIEventListener) event -> {
                updateFromMouse(event);
                startDrag(this, null);
                event.stopPropagation();
            });
            addEventListener(UIEvents.DRAG_SOURCE_UPDATE, (UIEventListener) this::updateFromMouse);
        }

        private void updateFromMouse(UIEvent event) {
            float width = getContentWidth();
            if (width <= 0) {
                return;
            }
            float local = getLocalMouse(event.x, event.y).x - getContentX();
            hsb[channel] = Math.max(0f, Math.min(1f, local / width));
        }

        @Override
        protected void draw(Gui2D graphics) {
            UiPainters.colourBar(graphics, hsb, channel, (int) getPositionX(), (int) getPositionY(),
                    (int) getSizeWidth(), (int) getSizeHeight());
        }
    }

    private final class Swatch extends WFUiElement {
        @Override
        protected void draw(Gui2D graphics) {
            UiPainters.colourSwatch(graphics, hsb, (int) getPositionX(), (int) getPositionY(),
                    (int) getSizeWidth(), (int) getSizeHeight());
        }
    }
}
