package com.flansmod.warforge.client;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.render.DynamicTextures;
import com.flansmod.warforge.client.render.Images;
import com.flansmod.warforge.client.util.ColorUtil;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.compat.Ids;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ClientFlagRegistry {
    /** Upper bound on the parts of one flag image; the server splits at 30 kB. */
    private static final int MAX_FLAG_PARTS = 256;
    private static final int MAX_FLAG_SIDE = 4096;
    private static final String TEXTURE_PATH = "dynamic/flag/";

    private static final Object2ObjectOpenHashMap<String, ResourceLocation> TEXTURES = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, int[]> DIMENSIONS = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, FlagAssembly> ASSEMBLIES = new Object2ObjectOpenHashMap<>();
    private static final ObjectArrayList<String> AVAILABLE_FLAGS = new ObjectArrayList<>();
    private static final List<String> AVAILABLE_FLAGS_VIEW = Collections.unmodifiableList(AVAILABLE_FLAGS);

    private ClientFlagRegistry() {
    }

    /** Render thread. */
    public static void clear() {
        for (ResourceLocation texture : TEXTURES.values()) {
            DynamicTextures.release(texture);
        }
        TEXTURES.clear();
        DIMENSIONS.clear();
        ASSEMBLIES.clear();
        AVAILABLE_FLAGS.clear();
    }

    public static void setAvailableFlags(List<String> flags) {
        AVAILABLE_FLAGS.clear();
        AVAILABLE_FLAGS.addAll(flags);
    }

    /** Live view; valid until the next manifest. */
    public static List<String> getAvailableFlags() {
        return AVAILABLE_FLAGS_VIEW;
    }

    public static void receiveCustomFlagChunk(String flagId, int width, int height, int partIndex, int totalParts, byte[] data) {
        if (width <= 0 || width > MAX_FLAG_SIDE || height <= 0 || height > MAX_FLAG_SIDE
                || totalParts < 1 || totalParts > MAX_FLAG_PARTS || partIndex < 0 || partIndex >= totalParts) {
            ASSEMBLIES.remove(flagId);
            WarForgeMod.LOGGER.warn("Dropping malformed flag chunk {}/{} ({}x{}) of flag {}", partIndex, totalParts, width, height, flagId);
            return;
        }
        FlagAssembly assembly = ASSEMBLIES.get(flagId);
        if (assembly == null || assembly.parts.length != totalParts) {
            assembly = new FlagAssembly(totalParts);
            ASSEMBLIES.put(flagId, assembly);
        }
        assembly.parts[partIndex] = data;
        if (assembly.isComplete()) {
            ASSEMBLIES.remove(flagId);
            byte[] bytes = assembly.join();
            ClientCompat.runLater(() -> registerCustomFlag(flagId, width, height, bytes));
        }
    }

    public static ResourceLocation getFlagTexture(String flagId) {
        if (flagId == null || flagId.isEmpty()) {
            return null;
        }
        return TEXTURES.get(flagId);
    }

    public static int[] getFlagDimensions(String flagId) {
        if (flagId == null || flagId.isEmpty()) {
            return null;
        }
        return DIMENSIONS.get(flagId);
    }

    private static void registerCustomFlag(String flagId, int width, int height, byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                WarForgeMod.LOGGER.warn("Flag {} is not a decodable image", flagId);
                return;
            }
            register(flagId, bufferedImageToNativeImage(image), width, height);
        } catch (IOException e) {
            WarForgeMod.LOGGER.warn("Could not read the image of flag {}", flagId, e);
        }
    }

    private static ResourceLocation register(String flagId, NativeImage image, int width, int height) {
        ResourceLocation location = Ids.of(Tags.MODID, TEXTURE_PATH + escape(flagId));
        DynamicTextures.register(location, image);
        TEXTURES.put(flagId, location);
        DIMENSIONS.put(flagId, new int[]{width, height});
        return location;
    }

    /** Chars outside [a-z0-9.-] -> "_" + 4 hex digits: distinct ids never share a texture. */
    static String escape(String flagId) {
        StringBuilder out = new StringBuilder(flagId.length());
        for (int i = 0; i < flagId.length(); i++) {
            char c = flagId.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '.' || c == '-') {
                out.append(c);
            } else {
                out.append('_').append(String.format(Locale.ROOT, "%04x", (int) c));
            }
        }
        return out.toString();
    }

    private static NativeImage bufferedImageToNativeImage(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
        NativeImage nativeImage = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Images.setPixelABGR(nativeImage, x, y, ColorUtil.swapRedBlue(argb[y * w + x]));
            }
        }
        return nativeImage;
    }

    private static final class FlagAssembly {
        private final byte[][] parts;

        private FlagAssembly(int totalParts) {
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
