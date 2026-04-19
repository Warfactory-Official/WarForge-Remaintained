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

        int baseTextWidth = font.getStringWidth(this.title);
        if (this.subtitle != null) {
            baseTextWidth = Math.max(baseTextWidth, font.getStringWidth(this.subtitle));
        }

        int iconPadding = (this.playerId != null) ? 35 : 15;
        int targetWidth = Math.min(250, Math.max(160, baseTextWidth + iconPadding + 10));
        int textWidth = targetWidth - iconPadding - 5;

        toastGui.getMinecraft().getTextureManager().bindTexture(TEXTURE_TOASTS);
        GlStateManager.color(1.0F, 1.0F, 1.0F);

        toastGui.drawTexturedModalRect(0, 0, 0, 0, 150, 32);
        toastGui.drawTexturedModalRect(targetWidth - 10, 0, 150, 0, 10, 32);
        if (targetWidth > 160) {
            for (int x = 150; x < targetWidth - 10; x++) {
                toastGui.drawTexturedModalRect(x, 0, 150, 0, 1, 32);
            }
        }

        int stripeColor = 0xFF000000 | (this.accentColor & 0x00FFFFFF);
        Gui.drawRect(0, 0, 4, 32, stripeColor);

        int textX = 10;
        if (this.playerId != null) {
            textX = 30;
            ResourceLocation face = SkinUtil.getPlayerFace(this.playerId);
            toastGui.getMinecraft().getTextureManager().bindTexture(face);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
            Gui.drawScaledCustomSizeModalRect(10, 8, 0, 0, 8, 8, 16, 16, 8, 8);
        }

        if (this.subtitle == null || this.subtitle.isEmpty()) {
            String drawTitle = font.trimStringToWidth(this.title, textWidth);
            font.drawString(drawTitle, textX, 12, 0xFFF2C84B);
        } else {
            String drawTitle = font.trimStringToWidth(this.title, textWidth);
            font.drawString(drawTitle, textX, 7, 0xFFF2C84B);

            List<String> subLines = font.listFormattedStringToWidth(this.subtitle, textWidth);
            int lineY = 18;
            int maxLines = 1;

            for (int i = 0; i < Math.min(subLines.size(), maxLines + 1); i++) {
                if (i > 1) break;
                String lineText = subLines.get(i);
                if (i == 1 && subLines.size() > 2) lineText = font.trimStringToWidth(lineText, textWidth - 8) + "...";

                font.drawString(lineText, textX, lineY, 0xFFFFFFFF);
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
