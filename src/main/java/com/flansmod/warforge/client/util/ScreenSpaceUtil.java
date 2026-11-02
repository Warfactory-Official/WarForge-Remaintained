package com.flansmod.warforge.client.util;

import net.minecraft.client.Minecraft;

public final class ScreenSpaceUtil {
    private static final int BOTTOM_ANCHOR_RESERVED = 43;

    private static int screenWidth;
    private static int screenHeight;
    private static int topLeftOffset, topRightOffset, topOffset;
    private static int bottomLeftOffset, bottomRightOffset, bottomOffset;

    private ScreenSpaceUtil() {
    }

    public static int textHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    public static int screenWidth() {
        return screenWidth;
    }

    public static int screenHeight() {
        return screenHeight;
    }

    public static void resetOffsets() {
        var window = Minecraft.getInstance().getWindow();
        resetOffsets(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    public static void resetOffsets(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        topOffset = topLeftOffset = topRightOffset = 0;
        bottomOffset = height - BOTTOM_ANCHOR_RESERVED;
        bottomLeftOffset = bottomRightOffset = height;
    }

    public static int centerX(int screenWidth, int elementWidth) {
        return (screenWidth - elementWidth) / 2;
    }

    public static int centerY(int screenHeight, int elementHeight) {
        return (screenHeight - elementHeight) / 2;
    }

    public static boolean isTop(ScreenPos pos) {
        return switch (pos) {
            case TOP, TOP_LEFT, TOP_RIGHT -> true;
            default -> false;
        };
    }

    public static int getXOffset(ScreenPos pos, int offset) {
        return switch (pos) {
            case TOP_RIGHT, BOTTOM_RIGHT -> -offset;
            case TOP, BOTTOM -> 0;
            default -> offset;
        };
    }

    public static int getYOffset(ScreenPos pos, int offset) {
        return switch (pos) {
            case TOP, TOP_RIGHT, TOP_LEFT -> offset;
            default -> -offset;
        };
    }

    public static int getX(ScreenPos pos, int elementWidth) {
        return switch (pos) {
            case TOP_LEFT, BOTTOM_LEFT -> 0;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - elementWidth;
            default -> centerX(screenWidth, elementWidth);
        };
    }

    public static int getY(ScreenPos pos, int elementHeight) {
        return switch (pos) {
            case TOP -> {
                int offset = topOffset;
                topOffset += elementHeight;
                yield offset;
            }
            case TOP_LEFT -> {
                int offset = topLeftOffset;
                topLeftOffset += elementHeight;
                yield offset;
            }
            case TOP_RIGHT -> {
                int offset = topRightOffset;
                topRightOffset += elementHeight;
                yield offset;
            }
            case BOTTOM -> {
                bottomOffset -= elementHeight;
                yield bottomOffset;
            }
            case BOTTOM_LEFT -> {
                bottomLeftOffset -= elementHeight;
                yield bottomLeftOffset;
            }
            case BOTTOM_RIGHT -> {
                bottomRightOffset -= elementHeight;
                yield bottomRightOffset;
            }
        };
    }

    public static int currentY(ScreenPos pos) {
        return switch (pos) {
            case TOP -> topOffset;
            case TOP_LEFT -> topLeftOffset;
            case TOP_RIGHT -> topRightOffset;
            case BOTTOM -> bottomOffset;
            case BOTTOM_LEFT -> bottomLeftOffset;
            case BOTTOM_RIGHT -> bottomRightOffset;
        };
    }

    public static void incrementY(ScreenPos pos, int amount) {
        switch (pos) {
            case TOP -> topOffset += amount;
            case TOP_LEFT -> topLeftOffset += amount;
            case TOP_RIGHT -> topRightOffset += amount;
            case BOTTOM -> bottomOffset -= amount;
            case BOTTOM_LEFT -> bottomLeftOffset -= amount;
            case BOTTOM_RIGHT -> bottomRightOffset -= amount;
        }
    }

    public static boolean shouldCenterX(ScreenPos pos) {
        return pos == ScreenPos.TOP || pos == ScreenPos.BOTTOM;
    }

    public enum ScreenPos {
        TOP_LEFT, BOTTOM_LEFT, TOP, BOTTOM, TOP_RIGHT, BOTTOM_RIGHT;

        public static ScreenPos fromString(String name) {
            for (ScreenPos pos : values()) {
                if (pos.name().equalsIgnoreCase(name)) {
                    return pos;
                }
            }
            return TOP;
        }
    }
}
