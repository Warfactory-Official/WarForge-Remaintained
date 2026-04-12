package com.flansmod.warforge.api.modularui;

import net.minecraft.client.gui.ScaledResolution;

public class ChunkMapViewport {
    public final int totalSize;
    public final int visibleSize;
    public final int startX;
    public final int startZ;

    private ChunkMapViewport(int totalSize, int visibleSize, int startX, int startZ) {
        this.totalSize = totalSize;
        this.visibleSize = visibleSize;
        this.startX = startX;
        this.startZ = startZ;
    }

    public static ChunkMapViewport create(
            int totalSize,
            int minVisible,
            int maxVisible,
            int cellSize,
            int screenWidth,
            int screenHeight,
            int chromeWidth,
            int chromeHeight,
            int pageX,
            int pageZ
    ) {
        int visibleByWidth = Math.max(minVisible, (screenWidth - chromeWidth) / cellSize);
        int visibleByHeight = Math.max(minVisible, (screenHeight - chromeHeight) / cellSize);
        int visibleSize = Math.min(totalSize, Math.min(maxVisible, Math.min(visibleByWidth, visibleByHeight)));
        if ((visibleSize & 1) == 0) {
            visibleSize--;
        }
        visibleSize = Math.max(minVisible, Math.min(totalSize, visibleSize));

        int maxStart = Math.max(0, totalSize - visibleSize);
        int defaultStart = Math.max(0, (totalSize - visibleSize) / 2);
        int startX = clamp(pageX < 0 ? defaultStart : pageX, 0, maxStart);
        int startZ = clamp(pageZ < 0 ? defaultStart : pageZ, 0, maxStart);
        return new ChunkMapViewport(totalSize, visibleSize, startX, startZ);
    }

    public boolean canPanWest() { return startX > 0; }
    public boolean canPanEast() { return startX + visibleSize < totalSize; }
    public boolean canPanNorth() { return startZ > 0; }
    public boolean canPanSouth() { return startZ + visibleSize < totalSize; }

    public int step() {
        return Math.max(1, visibleSize - 2);
    }

    public int panWest() { return Math.max(0, startX - step()); }
    public int panEast() { return Math.min(totalSize - visibleSize, startX + step()); }
    public int panNorth() { return Math.max(0, startZ - step()); }
    public int panSouth() { return Math.min(totalSize - visibleSize, startZ + step()); }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int recommendedCellSize(ScaledResolution scaled) {
        int minDim = Math.min(scaled.getScaledWidth(), scaled.getScaledHeight());
        if (minDim <= 260) return 28;
        if (minDim <= 340) return 36;
        if (minDim <= 430) return 44;
        if (minDim <= 560) return 52;
        return 64;
    }
}
