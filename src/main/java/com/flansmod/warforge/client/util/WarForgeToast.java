package com.flansmod.warforge.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarForgeToast implements Toast {
    private static final int LINE_HEIGHT = 9;

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

    // Recomputed each draw so width()/height() report the wrapped box dimensions to ToastComponent.
    private int targetWidth = 300;
    private int targetHeight = 32;

    public WarForgeToast(String token, String title, @Nullable String subtitle, int accentColor, long displayTimeMs, @Nullable UUID playerId) {
        this.token = token;
        this.title = title;
        this.subtitle = subtitle;
        this.accentColor = accentColor;
        this.displayTimeMs = displayTimeMs;
        this.playerId = playerId;
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long delta) {
        if (this.newDisplay) {
            this.firstDrawTime = delta;
            this.newDisplay = false;
        }

        Minecraft mc = toastComponent.getMinecraft();
        Font font = mc.font;

        int iconPadding = (this.playerId != null) ? 35 : 12;
        int rightPadding = 15;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int maxWidth = Math.max(160, screenWidth - 8);

        int titlePixelWidth = font.width(this.title);
        int desiredWidth = Math.max(300, titlePixelWidth + iconPadding + rightPadding);
        this.targetWidth = Math.min(desiredWidth, maxWidth);

        int textWidthLimit = Math.max(1, this.targetWidth - iconPadding - 10);

        List<FormattedCharSequence> titleLines = font.split(Component.literal(this.title), textWidthLimit);
        List<FormattedCharSequence> bodyLines = new ArrayList<>();
        if (this.subtitle != null && !this.subtitle.isEmpty()) {
            bodyLines = font.split(Component.literal(this.subtitle), textWidthLimit);
        }

        int contentHeight = (titleLines.size() + bodyLines.size()) * LINE_HEIGHT;
        this.targetHeight = Math.max(32, contentHeight + 14);

        guiGraphics.fill(0, 0, this.targetWidth, this.targetHeight, 0xFF171B1F);

        int stripeColor = 0xFF000000 | (this.accentColor & 0x00FFFFFF);
        guiGraphics.fill(0, 0, 4, this.targetHeight, stripeColor);

        int textX = iconPadding;
        if (this.playerId != null) {
            int iconY = (this.targetHeight - 8) / 2;
            SkinUtil.drawFace(guiGraphics, this.playerId, 10, iconY, 8);
        }

        int textY = (this.targetHeight - contentHeight) / 2;
        for (FormattedCharSequence line : titleLines) {
            guiGraphics.drawString(font, line, textX, textY, 0xFFF2C84B, false);
            textY += LINE_HEIGHT;
        }
        for (FormattedCharSequence line : bodyLines) {
            guiGraphics.drawString(font, line, textX, textY, 0xFFFFFFFF, false);
            textY += LINE_HEIGHT;
        }

        return delta - this.firstDrawTime < this.displayTimeMs ? Visibility.SHOW : Visibility.HIDE;
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
    }
}
