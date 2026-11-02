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
import javax.annotation.Nullable;
import java.util.List;

public class Label extends Widget {
    private static final int LINE_HEIGHT = 9;
    private static final int LINE_STEP = 10;
    private static final Vector4f SCISSOR_MIN = new Vector4f();
    private static final Vector4f SCISSOR_MAX = new Vector4f();

    private final Component text;
    private boolean shadow;
    private float scale = 1f;
    private int wrapWidth = -1;
    private int clipWidth = -1;
    private int boxWidth = -1;
    private int boxHeight = -1;
    private int align;
    private int lineCount = 1;
    private @Nullable List<FormattedCharSequence> lines;
    private int[] lineWidths = new int[0];

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

    public Label wrap(int width) {
        this.wrapWidth = width;
        resize();
        return this;
    }

    public Label clip(int width) {
        this.clipWidth = width;
        resize();
        return this;
    }

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

    public int lineCount() {
        return lineCount;
    }

    public int naturalWidth() {
        if (LDLib.isRemote()) {
            return ClientText.width(text);
        }
        return text.getString().length() * 6;
    }

    private void resize() {
        lines = null;
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

    private void layoutLines(Font font) {
        List<FormattedCharSequence> split = wrapWidth >= 0 && boxWidth < 0
                ? font.split(text, Math.max(1, (int) (wrapWidth / scale)))
                : List.of(text.getVisualOrderText());
        int[] widths = new int[split.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = Mth.ceil(font.width(split.get(i)) * scale);
        }
        lines = split;
        lineWidths = widths;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        Font font = Minecraft.getInstance().font;
        if (lines == null) {
            layoutLines(font);
        }
        int x = getPositionX();
        int y = getPositionY();
        int width = getSizeWidth();
        int height = getSizeHeight();

        boolean cut = clipWidth >= 0 || boxWidth >= 0;
        int textHeight = Mth.ceil((lines.size() * LINE_STEP - 1) * scale);
        int top = boxHeight >= 0 ? y + (height - textHeight) / 2 + 1 : y;

        if (cut) {
            var pose = graphics.pose().last().pose();
            pose.transform(SCISSOR_MIN.set(x, y - 1, 0, 1));
            pose.transform(SCISSOR_MAX.set(x + width, y + height + 1, 0, 1));
            graphics.enableScissor((int) SCISSOR_MIN.x, (int) SCISSOR_MIN.y, (int) SCISSOR_MAX.x, (int) SCISSOR_MAX.y);
        }
        int lineY = top;
        for (int i = 0; i < lines.size(); i++) {
            int lineWidth = lineWidths[i];
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
            if (scale == 1f) {
                graphics.drawString(font, lines.get(i), lineX, lineY, 0xFFFFFFFF, shadow);
            } else {
                graphics.pose().pushPose();
                graphics.pose().translate(lineX, lineY, 0);
                graphics.pose().scale(scale, scale, 1f);
                graphics.drawString(font, lines.get(i), 0, 0, 0xFFFFFFFF, shadow);
                graphics.pose().popPose();
            }
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
