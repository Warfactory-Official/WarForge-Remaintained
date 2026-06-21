package com.flansmod.warforge.client.util;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastComponent;

import javax.annotation.Nullable;
import java.util.UUID;

public final class WarForgeNotifications {
    public static final int COLOR_INFO = 0x708A97;
    public static final int COLOR_SUCCESS = 0x55AA55;
    public static final int COLOR_WARNING = 0xC79A3A;
    public static final int COLOR_DANGER = 0xB34747;
    public static final long DEFAULT_DURATION_MS = 5000L;

    private WarForgeNotifications() {
    }

    public static void show(String token, String title, @Nullable String subtitle, int accentColor) {
        show(token, title, subtitle, accentColor, DEFAULT_DURATION_MS, null);
    }

    public static void show(String token, String title, @Nullable String subtitle, int accentColor, long durationMs) {
        show(token, title, subtitle, accentColor, durationMs, null);
    }

    public static void show(String token, String title, @Nullable String subtitle, int accentColor, long durationMs, @Nullable UUID playerId) {
        Minecraft minecraft = Minecraft.getInstance();
        ToastComponent toasts = minecraft.getToasts();

        String resolvedToken = token == null || token.isEmpty() ? "warforge_notification_" + Util.getMillis() : token;
        WarForgeToast existing = toasts.getToast(WarForgeToast.class, resolvedToken);
        if (existing == null) {
            toasts.addToast(new WarForgeToast(resolvedToken, title, subtitle, accentColor, durationMs, playerId));
        } else {
            existing.update(title, subtitle, accentColor, durationMs, playerId);
        }
    }
}
