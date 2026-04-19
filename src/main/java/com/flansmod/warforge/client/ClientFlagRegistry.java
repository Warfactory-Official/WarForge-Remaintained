package com.flansmod.warforge.client;

import com.flansmod.warforge.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class ClientFlagRegistry {
    private static final HashMap<String, ResourceLocation> textures = new HashMap<String, ResourceLocation>();
    private static final HashMap<String, int[]> dimensions = new HashMap<String, int[]>();
    private static final HashMap<String, FlagAssembly> assemblies = new HashMap<String, FlagAssembly>();
    private static final ArrayList<String> availableFlags = new ArrayList<String>();

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
            return new ResourceLocation(Tags.MODID, "textures/flags/default/" + flagId.substring("default:".length()) + ".png");
        }
        return textures.get(flagId);
    }

    public static int[] getFlagDimensions(String flagId) {
        if (flagId == null || flagId.isEmpty()) {
            return null;
        }
        if (flagId.startsWith("default:")) {
            return dimensions.computeIfAbsent(flagId, ClientFlagRegistry::loadDefaultDimensions);
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

    private static int[] loadDefaultDimensions(String flagId) {
        ResourceLocation texture = getFlagTexture(flagId);
        if (texture == null) {
            return null;
        }
        try (InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(texture).getInputStream()) {
            BufferedImage image = ImageIO.read(stream);
            if (image != null) {
                return new int[]{image.getWidth(), image.getHeight()};
            }
        } catch (IOException ignored) {
        }
        return null;
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
