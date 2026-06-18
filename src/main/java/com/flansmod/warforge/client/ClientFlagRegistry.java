package com.flansmod.warforge.client;

import com.flansmod.warforge.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public final class ClientFlagRegistry {
    private static final HashMap<String, ResourceLocation> textures = new HashMap<String, ResourceLocation>();
    private static final HashMap<String, int[]> dimensions = new HashMap<String, int[]>();
    private static final HashMap<String, FlagAssembly> assemblies = new HashMap<String, FlagAssembly>();
    private static final ArrayList<String> availableFlags = new ArrayList<String>();
    private static final int DEFAULT_FLAG_SIZE = 64;
    private static final HashMap<String, Integer> DEFAULT_COLORS = new HashMap<String, Integer>();

    static {
        DEFAULT_COLORS.put("white", 0xF9FFFE);
        DEFAULT_COLORS.put("orange", 0xF9801D);
        DEFAULT_COLORS.put("magenta", 0xC74EBD);
        DEFAULT_COLORS.put("light_blue", 0x3AB3DA);
        DEFAULT_COLORS.put("yellow", 0xFED83D);
        DEFAULT_COLORS.put("lime", 0x80C71F);
        DEFAULT_COLORS.put("pink", 0xF38BAA);
        DEFAULT_COLORS.put("gray", 0x474F52);
        DEFAULT_COLORS.put("light_gray", 0x9D9D97);
        DEFAULT_COLORS.put("cyan", 0x169C9C);
        DEFAULT_COLORS.put("purple", 0x8932B8);
        DEFAULT_COLORS.put("blue", 0x3C44AA);
        DEFAULT_COLORS.put("brown", 0x835432);
        DEFAULT_COLORS.put("green", 0x5E7C16);
        DEFAULT_COLORS.put("red", 0xB02E26);
        DEFAULT_COLORS.put("black", 0x1D1D21);
    }

    private ClientFlagRegistry() {
    }

    public static void clear() {
        for (ResourceLocation texture : textures.values()) {
            if (texture.getNamespace().equals(Tags.MODID)) {
                Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().getTextureManager().deleteTexture(texture));
            }
        }
        textures.clear();
        dimensions.clear();
        assemblies.clear();
        availableFlags.clear();
    }

    public static void setAvailableFlags(List<String> flags) {
        availableFlags.clear();
        availableFlags.addAll(flags);
    }

    public static List<String> getAvailableFlags() {
        return new ArrayList<String>(availableFlags);
    }

    public static void receiveCustomFlagChunk(String flagId, int width, int height, int partIndex, int totalParts, byte[] data) {
        FlagAssembly assembly = assemblies.computeIfAbsent(flagId, ignored -> new FlagAssembly(totalParts, width, height));
        assembly.parts[partIndex] = data;
        if (assembly.isComplete()) {
            assemblies.remove(flagId);
            byte[] bytes = assembly.join();
            Minecraft.getMinecraft().addScheduledTask(() -> registerCustomFlag(flagId, width, height, bytes));
        }
    }

    public static ResourceLocation getFlagTexture(String flagId) {
        if (flagId == null || flagId.isEmpty()) {
            return null;
        }
        if (flagId.startsWith("default:")) {
            ResourceLocation cached = textures.get(flagId);
            return cached != null ? cached : generateDefaultFlag(flagId);
        }
        return textures.get(flagId);
    }

    public static int[] getFlagDimensions(String flagId) {
        if (flagId == null || flagId.isEmpty()) {
            return null;
        }
        if (flagId.startsWith("default:") && !dimensions.containsKey(flagId)) {
            generateDefaultFlag(flagId);
        }
        return dimensions.get(flagId);
    }

    private static void registerCustomFlag(String flagId, int width, int height, byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return;
            }
            ResourceLocation location = new ResourceLocation(Tags.MODID, "dynamic/flag/" + Integer.toUnsignedString(flagId.hashCode()));
            Minecraft.getMinecraft().getTextureManager().loadTexture(location, new DynamicTexture(image));
            textures.put(flagId, location);
            dimensions.put(flagId, new int[]{width, height});
        } catch (IOException ignored) {
        }
    }

    private static ResourceLocation generateDefaultFlag(String flagId) {
        String name = flagId.substring("default:".length());
        int argb = 0xFF000000 | (parseColor(name) & 0xFFFFFF);
        int[] pixels = new int[DEFAULT_FLAG_SIZE * DEFAULT_FLAG_SIZE];
        Arrays.fill(pixels, argb);
        BufferedImage image = new BufferedImage(DEFAULT_FLAG_SIZE, DEFAULT_FLAG_SIZE, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, DEFAULT_FLAG_SIZE, DEFAULT_FLAG_SIZE, pixels, 0, DEFAULT_FLAG_SIZE);
        ResourceLocation location = new ResourceLocation(Tags.MODID, "dynamic/flag/default_" + name.toLowerCase(Locale.ROOT));
        Minecraft.getMinecraft().getTextureManager().loadTexture(location, new DynamicTexture(image));
        textures.put(flagId, location);
        dimensions.put(flagId, new int[]{DEFAULT_FLAG_SIZE, DEFAULT_FLAG_SIZE});
        return location;
    }

    private static int parseColor(String name) {
        if (name == null) {
            return 0x999999;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        Integer named = DEFAULT_COLORS.get(key);
        if (named != null) {
            return named;
        }
        try {
            return Integer.parseInt(key, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return 0x999999;
        }
    }

    private static final class FlagAssembly {
        private final byte[][] parts;

        private FlagAssembly(int totalParts, int width, int height) {
            this.parts = new byte[totalParts][];
        }

        private boolean isComplete() {
            for (byte[] part : parts) {
                if (part == null) {
                    return false;
                }
            }
            return true;
        }

        private byte[] join() {
            int total = 0;
            for (byte[] part : parts) {
                total += part.length;
            }
            byte[] joined = new byte[total];
            int offset = 0;
            for (byte[] part : parts) {
                System.arraycopy(part, 0, joined, offset, part.length);
                offset += part.length;
            }
            return joined;
        }
    }
}
