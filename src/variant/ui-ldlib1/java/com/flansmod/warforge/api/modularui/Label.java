package com.flansmod.warforge.api.modularui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * A static text label, positioned absolutely (the ModularUI Text widget of the 1.20.1 menus). By default it is one
 * line at its natural size. {@link #wrap} wraps it to a width, {@link #clip} keeps one line cut to a width that
 * scrolls while hovered (ScrollingTextWidget), and {@link #box} gives it a fixed box in which the text is vertically
 * centred. On the server it only estimates its size (the size never decides the widget tree).
 */
public class Label extends Widget {
    private static final int LINE_HEIGHT = 9;
    private static final int LINE_STEP = 10;

    private final Component text;
    private boolean shadow;
    private float scale = 1f;
    private int wrapWidth = -1;
    private int clipWidth = -1;
    private int boxWidth = -1;
    private int boxHeight = -1;
    private int align;
    private int lineCount = 1;

    public Label(Component text) {
        super(0, 0, 0, LINE_HEIGHT);
        this.text = text;
        resize();
    }

    public static Label text(String value, int rgb) {
        return new Label(styled(value, rgb, false));
    }

    public static Label bold(String value, int rgb) {
        return new Label(styled(value, rgb, true));
    }

    private static MutableComponent styled(String value, int rgb, boolean bold) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF));
        if (bold) {
            style = style.withBold(true);
        }
        return Component.literal(value).withStyle(style);
    }

    public Label at(int x, int y) {
        setSelfPosition(x, y);
        return this;
    }

    public Label shadow() {
        this.shadow = true;
        return this;
    }

    public Label scale(float scale) {
        this.scale = scale;
        resize();
        return this;
    }

    /** Wraps the text to the width; the height follows the line count. */
    public Label wrap(int width) {
        this.wrapWidth = width;
        resize();
        return this;
    }

    /** One line cut to the width; scrolls while hovered when it is longer. */
    public Label clip(int width) {
        this.clipWidth = width;
        resize();
        return this;
    }

    /** A fixed box; the text is vertically centred in it (and cut to its width). */
    public Label box(int width, int height) {
        this.boxWidth = width;
        this.boxHeight = height;
        resize();
        return this;
    }

    public Label center() {
        this.align = 1;
        return this;
    }

    public Label right() {
        this.align = 2;
        return this;
    }

    public Label tooltip(String... lines) {
        setHoverTooltips(lines);
        return this;
    }

    public Component getText() {
        return text;
    }

    /** The number of lines after wrapping. */
    public int lineCount() {
        return lineCount;
    }

    /** The unscaled natural width of the text. */
    public int naturalWidth() {
        if (LDLib.isRemote()) {
            return ClientText.width(text);
        }
        return text.getString().length() * 6;
    }

    private void resize() {
        int natural = Mth.ceil(naturalWidth() * scale);
        int lineHeight = Mth.ceil(LINE_HEIGHT * scale);
        if (boxWidth >= 0) {
            setSize(new Size(boxWidth, boxHeight));
            lineCount = 1;
        } else if (wrapWidth >= 0) {
            lineCount = LDLib.isRemote()
                    ? Math.max(1, ClientText.lineCount(text, Math.max(1, (int) (wrapWidth / scale))))
                    : Math.max(1, (natural + wrapWidth - 1) / Math.max(1, wrapWidth));
            setSize(new Size(wrapWidth, Mth.ceil((lineCount * LINE_STEP - 1) * scale)));
        } else if (clipWidth >= 0) {
            lineCount = 1;
            setSize(new Size(clipWidth, lineHeight));
        } else {
            lineCount = 1;
            setSize(new Size(natural, lineHeight));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        Font font = Minecraft.getInstance().font;
        int x = getPositionX();
        int y = getPositionY();
        int width = getSizeWidth();
        int height = getSizeHeight();

        List<FormattedCharSequence> lines = wrapWidth >= 0 && boxWidth < 0
                ? font.split(text, Math.max(1, (int) (wrapWidth / scale)))
                : Collections.singletonList(text.getVisualOrderText());
        boolean cut = clipWidth >= 0 || boxWidth >= 0;
        int textHeight = Mth.ceil((lines.size() * LINE_STEP - 1) * scale);
        int top = boxHeight >= 0 ? y + (height - textHeight) / 2 + 1 : y;

        if (cut) {
            var pose = graphics.pose().last().pose();
            Vector4f min = pose.transform(new Vector4f(x, y - 1, 0, 1));
            Vector4f max = pose.transform(new Vector4f(x + width, y + height + 1, 0, 1));
            graphics.enableScissor((int) min.x, (int) min.y, (int) max.x, (int) max.y);
        }
        int lineY = top;
        for (FormattedCharSequence line : lines) {
            int lineWidth = Mth.ceil(font.width(line) * scale);
            int lineX = x;
            if (cut && lineWidth > width) {
                if (isMouseOverElement(mouseX, mouseY)) {
                    int overflow = lineWidth - width;
                    int period = overflow * 2 + 80;
                    int t = (int) ((System.currentTimeMillis() / 30) % period);
                    int offset = t < 20 ? 0 : t < 20 + overflow ? t - 20 : t < 60 + overflow ? overflow : overflow - (t - 60 - overflow);
                    lineX = x - Math.max(0, Math.min(overflow, offset));
                }
            } else if (align == 1) {
                lineX = x + (width - lineWidth) / 2;
            } else if (align == 2) {
                lineX = x + width - lineWidth;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(lineX, lineY, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.drawString(font, line, 0, 0, 0xFFFFFFFF, shadow);
            graphics.pose().popPose();
            lineY += Mth.ceil(LINE_STEP * scale);
        }
        if (cut) {
            graphics.disableScissor();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientText {
        static int width(Component text) {
            return Minecraft.getInstance().font.width(text);
        }

        static int lineCount(Component text, int width) {
            return Minecraft.getInstance().font.split(text, width).size();
        }
    }
}
