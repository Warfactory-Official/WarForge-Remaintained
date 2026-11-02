package com.flansmod.warforge.client.util;

public final class ColorUtil {
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int OPAQUE = 0xFF000000;
    private static final int BRIGHT_THRESHOLD = 128;

    private ColorUtil() {
    }

    public static int argb(int alpha, int rgb) {
        return (alpha & 0xFF) << 24 | rgb & RGB_MASK;
    }

    public static int opaque(int rgb) {
        return OPAQUE | rgb & RGB_MASK;
    }

    public static int swapRedBlue(int color) {
        return color & 0xFF00FF00 | (color & 0xFF) << 16 | color >> 16 & 0xFF;
    }

    public static int lighten(int rgb, float amount) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        r = Math.round(r + (255 - r) * amount);
        g = Math.round(g + (255 - g) * amount);
        b = Math.round(b + (255 - b) * amount);
        return r << 16 | g << 8 | b;
    }

    public static int darken(int rgb, float amount) {
        int r = Math.round((rgb >> 16 & 0xFF) * (1f - amount));
        int g = Math.round((rgb >> 8 & 0xFF) * (1f - amount));
        int b = Math.round((rgb & 0xFF) * (1f - amount));
        return r << 16 | g << 8 | b;
    }

    public static int shift(int rgb, int delta) {
        int r = clampChannel((rgb >> 16 & 0xFF) + delta);
        int g = clampChannel((rgb >> 8 & 0xFF) + delta);
        int b = clampChannel((rgb & 0xFF) + delta);
        return r << 16 | g << 8 | b;
    }

    public static boolean isBright(int color) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > BRIGHT_THRESHOLD;
    }

    private static int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
