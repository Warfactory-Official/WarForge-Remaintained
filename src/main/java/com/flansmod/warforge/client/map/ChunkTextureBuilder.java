package com.flansmod.warforge.client.map;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.Color4i;
import com.flansmod.warforge.client.render.DynamicTextures;
import com.flansmod.warforge.client.render.Images;
import com.flansmod.warforge.compat.Ids;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkTextureBuilder {
    public static final Map<String, RegisterTextureAction> PENDING = new ConcurrentHashMap<>();

    private static final int PADDED = 17;
    private static final double LIGHT_X = -1 / Math.sqrt(3);
    private static final double LIGHT_Y = 1 / Math.sqrt(3);
    private static final double LIGHT_Z = -1 / Math.sqrt(3);
    private static final float EXAGGERATION = 1.5f;

    private final int[] rawChunk;
    private final int[] heightMap;
    private final int maxHeight;
    private final int minHeight;
    private final int scale;
    private final String name;

    public ChunkTextureBuilder(int scale, String name, int[] rawChunk, int[] heightMap, int maxHeight, int minHeight) {
        this.scale = scale;
        this.name = name;
        this.rawChunk = rawChunk;
        this.heightMap = heightMap;
        this.maxHeight = maxHeight;
        this.minHeight = minHeight;
    }

    public static int[] scaleRGBAArray(int[] originalPixels, int originalWidth, int originalHeight, int scale) {
        int newWidth = originalWidth * scale;
        int[] scaledPixels = new int[newWidth * originalHeight * scale];
        for (int y = 0; y < originalHeight; y++) {
            for (int x = 0; x < originalWidth; x++) {
                int color = originalPixels[x + y * originalWidth];
                int startX = x * scale;
                int startY = y * scale;
                for (int dy = 0; dy < scale; dy++) {
                    int row = (startY + dy) * newWidth + startX;
                    for (int dx = 0; dx < scale; dx++) {
                        scaledPixels[row + dx] = color;
                    }
                }
            }
        }
        return scaledPixels;
    }

    public static void applyShadingWithHeight(int[] rawChunk, int[] heightMap, int width, int height) {
        int[] shaded = new int[rawChunk.length];
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int idx = x + z * width;
                int hL = heightMap[(x > 0 ? x - 1 : x) + z * width];
                int hR = heightMap[(x < width - 1 ? x + 1 : x) + z * width];
                int hU = heightMap[x + (z > 0 ? z - 1 : z) * width];
                int hD = heightMap[x + (z < height - 1 ? z + 1 : z) * width];

                double nx = -(hR - hL);
                double nz = -(hD - hU);
                double length = Math.sqrt(nx * nx + EXAGGERATION * EXAGGERATION + nz * nz);
                float shade = (float) ((nx * LIGHT_X + EXAGGERATION * LIGHT_Y + nz * LIGHT_Z) / length);
                shade = Math.max(0.0f, Math.min(1.0f, shade));
                float brightness = 0.5f + (float) Math.pow(shade, EXAGGERATION) * 0.5f;

                shaded[idx] = new Color4i(rawChunk[idx]).withGammaBrightness(brightness, false).toRGB();
            }
        }
        System.arraycopy(shaded, 0, rawChunk, 0, rawChunk.length);
    }

    public void build() {
        int paddedScaled = PADDED * scale;

        applyHeightMap(rawChunk, heightMap);
        int[] scaled = scaleRGBAArray(rawChunk, PADDED, PADDED, scale);
        applyShadingWithHeight(scaled, scaleRGBAArray(heightMap, PADDED, PADDED, 4), paddedScaled, paddedScaled);

        int size = 16 * scale;
        int[] finalBuffer = new int[size * size];
        for (int z = 0; z < size; z++) {
            System.arraycopy(scaled, (z + scale) * paddedScaled + scale, finalBuffer, z * size, size);
        }
        PENDING.put(name, new RegisterTextureAction(finalBuffer, size, name));
    }

    private void applyHeightMap(int[] colorBuffer, int[] heights) {
        int span = maxHeight - minHeight;
        if (span <= 0) {
            for (int i = 0; i < colorBuffer.length; i++) {
                colorBuffer[i] = Color4i.fromRGB(colorBuffer[i]).withHSVBrightness(0.8f).toRGB();
            }
            return;
        }

        float logSpan = (float) Math.log(span + 1);
        for (int i = 0; i < colorBuffer.length; i++) {
            int delta = Math.max(0, Math.min(span, heights[i] - minHeight));
            float brightness = 0.6f + (float) Math.log(delta + 1) / logSpan * 0.4f;
            colorBuffer[i] = Color4i.fromRGB(colorBuffer[i]).withHSVBrightness(brightness).toRGB();
        }
    }

    private static NativeImage toNativeImage(int[] argb, int size) {
        NativeImage nativeImage = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = argb[x + y * size];
                int a = (color >>> 24) & 0xFF;
                int r = (color >>> 16) & 0xFF;
                int g = (color >>> 8) & 0xFF;
                int b = color & 0xFF;
                Images.setPixelABGR(nativeImage, x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        return nativeImage;
    }

    public record RegisterTextureAction(int[] argbPixels, int size, String name) {
        public void register() {
            var location = Ids.of(Tags.MODID, name);
            DynamicTextures.release(location);
            DynamicTextures.register(location, toNativeImage(argbPixels, size));
        }
    }
}
