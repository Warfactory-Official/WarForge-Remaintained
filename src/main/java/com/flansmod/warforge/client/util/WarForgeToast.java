package com.flansmod.warforge.client.util;

import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.WFToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class WarForgeToast extends WFToast {
    private static final int LINE_HEIGHT = 9;
    private static final int BACKGROUND = 0xFF171B1F;
    private static final int TITLE_COLOR = 0xFFF2C84B;
    private static final int BODY_COLOR = 0xFFFFFFFF;
    private static final int STRIPE_WIDTH = 4;
    private static final int FACE_X = 10;
    private static final int FACE_SIZE = 20;
    private static final int ICON_PADDING = 12;
    private static final int ICON_PADDING_WITH_FACE = 35;
    private static final int RIGHT_PADDING = 15;
    private static final int TEXT_MARGIN = 10;
    private static final int DEFAULT_WIDTH = 300;
    private static final int MIN_WIDTH = 160;
    private static final int SCREEN_MARGIN = 8;
    private static final int MIN_HEIGHT = 32;
    private static final int VERTICAL_PADDING = 14;

    private final String token;
    private String title;
    @Nullable
    private String subtitle;
    private int accentColor;
    private long displayTimeMs;
    @Nullable
    private UUID playerId;
    private long firstDrawTime;
    private boolean newDisplay = true;

    private boolean layoutDirty = true;
    private int layoutScreenWidth = -1;
    private int textLeft;
    private int contentHeight;
    private List<FormattedCharSequence> titleLines = List.of();
    private List<FormattedCharSequence> bodyLines = List.of();
    private int targetWidth = DEFAULT_WIDTH;
    private int targetHeight = MIN_HEIGHT;

    public WarForgeToast(String token, String title, @Nullable String subtitle, int accentColor, long displayTimeMs, @Nullable UUID playerId) {
        this.token = token;
        this.title = title;
        this.subtitle = subtitle;
        this.accentColor = accentColor;
        this.displayTimeMs = displayTimeMs;
        this.playerId = playerId;
    }

    private void markStart(long delta) {
        if (this.newDisplay) {
            this.firstDrawTime = delta;
            this.newDisplay = false;
        }
    }

    @Override
    protected boolean isVisible(long delta) {
        markStart(delta);
        return delta - this.firstDrawTime < this.displayTimeMs;
    }

    @Override
    protected void draw(Gui2D guiGraphics, Font font, long delta) {
        markStart(delta);

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        if (layoutDirty || screenWidth != layoutScreenWidth) {
            layout(font, screenWidth);
        }

        guiGraphics.fill(0, 0, this.targetWidth, this.targetHeight, BACKGROUND);
        guiGraphics.fill(0, 0, STRIPE_WIDTH, this.targetHeight, 0xFF000000 | (this.accentColor & 0x00FFFFFF));

        int textX = textLeft;
        if (this.playerId != null) {
            SkinUtil.drawFace(guiGraphics, this.playerId, FACE_X, (this.targetHeight - FACE_SIZE) / 2, FACE_SIZE);
        }

        int textY = (this.targetHeight - contentHeight) / 2;
        for (int i = 0; i < titleLines.size(); i++) {
            guiGraphics.text(font, titleLines.get(i), textX, textY, TITLE_COLOR, false);
            textY += LINE_HEIGHT;
        }
        for (int i = 0; i < bodyLines.size(); i++) {
            guiGraphics.text(font, bodyLines.get(i), textX, textY, BODY_COLOR, false);
            textY += LINE_HEIGHT;
        }
    }

    private void layout(Font font, int screenWidth) {
        layoutDirty = false;
        layoutScreenWidth = screenWidth;
        textLeft = this.playerId != null ? ICON_PADDING_WITH_FACE : ICON_PADDING;
        int maxWidth = Math.max(MIN_WIDTH, screenWidth - SCREEN_MARGIN);
        this.targetWidth = Math.min(Math.max(DEFAULT_WIDTH, font.width(this.title) + textLeft + RIGHT_PADDING), maxWidth);

        int textWidthLimit = Math.max(1, this.targetWidth - textLeft - TEXT_MARGIN);
        titleLines = font.split(Component.literal(this.title), textWidthLimit);
        bodyLines = this.subtitle != null && !this.subtitle.isEmpty()
                ? font.split(Component.literal(this.subtitle), textWidthLimit) : List.of();

        contentHeight = (titleLines.size() + bodyLines.size()) * LINE_HEIGHT;
        this.targetHeight = Math.max(MIN_HEIGHT, contentHeight + VERTICAL_PADDING);
    }

    @Override
    public int width() {
        return this.targetWidth;
    }

    @Override
    public int height() {
        return this.targetHeight;
    }

    @Override
    public Object getToken() {
        return this.token;
    }

    public void update(String title, @Nullable String subtitle, int accentColor, long displayTimeMs, @Nullable UUID playerId) {
        this.title = title;
        this.subtitle = subtitle;
        this.accentColor = accentColor;
        this.displayTimeMs = displayTimeMs;
        this.playerId = playerId;
        this.newDisplay = true;
        this.layoutDirty = true;
    }
}
