package com.flansmod.warforge.client;

import net.minecraft.network.chat.Component;

public final class ClaimMapStyle {
    private static final float FILL_MAX_OPACITY = 0.4f;
    private static final float BORDER_MIN_OPACITY = 0.7f;
    private static final float OPAQUE = 255f;

    private ClaimMapStyle() {
    }

    public static float fillOpacity(int colour) {
        return Math.min(alpha(colour), FILL_MAX_OPACITY);
    }

    public static float borderOpacity(int colour) {
        return Math.max(alpha(colour), BORDER_MIN_OPACITY);
    }

    public static int fillAlpha(int colour) {
        return Math.round(fillOpacity(colour) * OPAQUE);
    }

    public static int borderAlpha(int colour) {
        return Math.round(borderOpacity(colour) * OPAQUE);
    }

    /** Hover text of a claimed chunk: position only, the sync carries no faction names. */
    public static Component label(int chunkX, int chunkZ) {
        return Component.translatable("warforge.client.map.claim_label", chunkX, chunkZ);
    }

    public static boolean sharesBorder(int colour, int neighbourColour) {
        return colour == neighbourColour;
    }

    private static float alpha(int colour) {
        return (colour >>> 24) / OPAQUE;
    }
}
