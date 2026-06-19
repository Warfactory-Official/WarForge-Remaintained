package com.flansmod.warforge.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
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

        Minecraft mc = toastGui.getMinecraft();
        FontRenderer font = mc.fontRenderer;

        int iconPadding = (this.playerId != null) ? 35 : 12;
        int rightPadding = 15;
        int lineHeight = 9;

        // Vanilla GuiToast anchors a 160px-wide slot flush against the right edge of the
        // screen and draws rightward, so anything wider would run off-screen. Cap the width
        // to the visible area (and shift the box left below) so long text wraps onto extra
        // lines instead of spilling past the screen edge.
        int screenWidth = new ScaledResolution(mc).getScaledWidth();
        int maxWidth = Math.max(160, screenWidth - 8);

        int titlePixelWidth = font.getStringWidth(this.title);
        int desiredWidth = Math.max(300, titlePixelWidth + iconPadding + rightPadding);
        int targetWidth = Math.min(desiredWidth, maxWidth);

        int textWidthLimit = Math.max(1, targetWidth - iconPadding - 10);

        List<String> titleLines = font.listFormattedStringToWidth(this.title, textWidthLimit);
        List<String> bodyLines = new ArrayList<>();
        if (this.subtitle != null && !this.subtitle.isEmpty()) {
            bodyLines = font.listFormattedStringToWidth(this.subtitle, textWidthLimit);
        }

        int contentHeight = (titleLines.size() + bodyLines.size()) * lineHeight;
        int targetHeight = Math.max(32, contentHeight + 14);

        // Shift left so the (now bounded) right edge lines up with the screen edge.
        GlStateManager.pushMatrix();
        GlStateManager.translate(160 - targetWidth, 0.0F, 0.0F);

        Gui.drawRect(0, 0, targetWidth, targetHeight, 0xFF171B1F);

        int stripeColor = 0xFF000000 | (this.accentColor & 0x00FFFFFF);
        Gui.drawRect(0, 0, 4, targetHeight, stripeColor);

        int textX = iconPadding;
        if (this.playerId != null) {
            ResourceLocation face = SkinUtil.getPlayerFace(this.playerId);
            mc.getTextureManager().bindTexture(face);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
            int iconY = (targetHeight - 8) / 2;
            Gui.drawScaledCustomSizeModalRect(10, iconY, 0, 0, 8, 8, 16, 16, 8, 8);
        }

        int textY = (targetHeight - contentHeight) / 2;
        for (String line : titleLines) {
            font.drawString(line, textX, textY, 0xFFF2C84B);
            textY += lineHeight;
        }
        for (String line : bodyLines) {
            font.drawString(line, textX, textY, 0xFFFFFFFF);
            textY += lineHeight;
        }

        GlStateManager.popMatrix();

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
