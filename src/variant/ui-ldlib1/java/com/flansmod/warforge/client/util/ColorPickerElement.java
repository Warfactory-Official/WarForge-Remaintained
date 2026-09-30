package com.flansmod.warforge.client.util;

import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.UiPainters;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.awt.Color;

/**
 * An HSB colour picker: three channel bars (hue, saturation, brightness) that are set by clicking or dragging, and a
 * preview swatch on the right (the 1.20.1 create-faction layout: 156px bars, 12px gap, swatch to the right).
 */
@OnlyIn(Dist.CLIENT)
public class ColorPickerElement extends Widget {
    private static final int BAR_HEIGHT = 12;
    private static final int BAR_GAP = 3;
    public static final int HEIGHT = BAR_HEIGHT * 3 + BAR_GAP * 2;

    private final int barWidth;
    private final int swatchGap;

    private final float[] hsb = new float[3];
    private int draggingChannel = -1;

    /** A picker at x, y with bars of barWidth, then swatchGap, then a swatch of swatchWidth. */
    public ColorPickerElement(int rgb, int x, int y, int barWidth, int swatchGap, int swatchWidth) {
        super(x, y, barWidth + swatchGap + swatchWidth, HEIGHT);
        this.barWidth = barWidth;
        this.swatchGap = swatchGap;
        Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
    }

    public int getRGB() {
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    private int barY(int channel) {
        return getPositionY() + channel * (BAR_HEIGHT + BAR_GAP);
    }

    private int channelAt(double mouseX, double mouseY) {
        int x = getPositionX();
        if (mouseX < x || mouseX >= x + barWidth) {
            return -1;
        }
        for (int channel = 0; channel < 3; channel++) {
            int y = barY(channel);
            if (mouseY >= y && mouseY < y + BAR_HEIGHT) {
                return channel;
            }
        }
        return -1;
    }

    private void updateChannel(int channel, double mouseX) {
        float width = barWidth;
        float local = (float) (mouseX - getPositionX());
        hsb[channel] = Math.max(0f, Math.min(1f, local / width));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int channel = channelAt(mouseX, mouseY);
        if (channel >= 0) {
            draggingChannel = channel;
            updateChannel(channel, mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingChannel >= 0) {
            updateChannel(draggingChannel, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingChannel >= 0) {
            draggingChannel = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        Gui2D gui = Gui2D.of(graphics);
        int x = getPositionX();
        for (int channel = 0; channel < 3; channel++) {
            UiPainters.colourBar(gui, hsb, channel, x, barY(channel), barWidth, BAR_HEIGHT);
        }
        UiPainters.colourSwatch(gui, hsb, x + barWidth + swatchGap, getPositionY(), getSizeWidth() - barWidth - swatchGap, HEIGHT);
    }
}
