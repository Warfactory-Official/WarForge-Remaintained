package com.flansmod.warforge.client.util;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SideOnly(Side.CLIENT)
public class WarForgeToast implements IToast {
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

    public WarForgeToast(String token, String title, @Nullable String subtitle, int accentColor, long displayTimeMs, @Nullable UUID playerId) {
        this.token = token;
        this.title = title;
        this.subtitle = subtitle;
        this.accentColor = accentColor;
        this.displayTimeMs = displayTimeMs;
        this.playerId = playerId;
    }

    @Override
    public Visibility draw(GuiToast toastGui, long delta) {
        if (this.newDisplay) {
            this.firstDrawTime = delta;
            this.newDisplay = false;
        }

        FontRenderer font = toastGui.getMinecraft().fontRenderer;

        int iconPadding = (this.playerId != null) ? 35 : 12;
        int titlePixelWidth = font.getStringWidth(this.title);

        int targetWidth = Math.max(300, titlePixelWidth + iconPadding + 15);
        int textWidthLimit = targetWidth - iconPadding - 10;

        List<String> wrappedLines = new ArrayList<>();
        if (this.subtitle != null && !this.subtitle.isEmpty()) {
            wrappedLines = font.listFormattedStringToWidth(this.subtitle, textWidthLimit);
        }

        int additionalLines = Math.max(0, wrappedLines.size() - 1);
        int targetHeight = 32 + (additionalLines * 9);

        Gui.drawRect(0, 0, targetWidth, targetHeight, 0xFF171B1F);

        int stripeColor = 0xFF000000 | (this.accentColor & 0x00FFFFFF);
        Gui.drawRect(0, 0, 4, targetHeight, stripeColor);

        int textX = iconPadding;
        if (this.playerId != null) {
            ResourceLocation face = SkinUtil.getPlayerFace(this.playerId);
            toastGui.getMinecraft().getTextureManager().bindTexture(face);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
            Gui.drawScaledCustomSizeModalRect(10, 8, 0, 0, 8, 8, 16, 16, 8, 8);
        }

        if (wrappedLines.isEmpty()) {
            int titleY = (targetHeight / 2) - 4;
            font.drawString(this.title, textX, titleY, 0xFFF2C84B);
        } else {
            font.drawString(this.title, textX, 7, 0xFFF2C84B);

            int lineY = 18;
            for (String line : wrappedLines) {
                font.drawString(line, textX, lineY, 0xFFFFFFFF);
                lineY += 9;
            }
        }

        return delta - this.firstDrawTime < this.displayTimeMs ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public Object getType() {
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
