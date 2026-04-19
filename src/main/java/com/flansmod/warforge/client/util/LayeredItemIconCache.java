package com.flansmod.warforge.client.util;

import com.flansmod.warforge.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LayeredItemIconCache {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final ConcurrentHashMap<String, ResourceLocation> CACHE = new ConcurrentHashMap<String, ResourceLocation>();

    private LayeredItemIconCache() {
    }

    @Nullable
    public static ResourceLocation getIcon(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        String key = makeKey(stack);
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        BufferedImage baked = bakeIcon(stack);
        if (baked == null) {
            return null;
        }

        ResourceLocation location = new ResourceLocation(Tags.MODID, "generated/vein/" + Integer.toUnsignedString(key.hashCode()));
        MC.getTextureManager().loadTexture(location, new DynamicTexture(baked));
        CACHE.put(key, location);
        return location;
    }

    public static void clear() {
        for (ResourceLocation location : CACHE.values()) {
            MC.getTextureManager().deleteTexture(location);
        }
        CACHE.clear();
    }

    @Nullable
    private static BufferedImage bakeIcon(ItemStack stack) {
        IBakedModel model = MC.getRenderItem().getItemModelWithOverrides(stack, null, null);
        List<Layer> layers = extractLayers(model, stack);
        if (layers.isEmpty()) {
            return null;
        }

        int width = 16;
        int height = 16;
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = combined.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.dispose();

        for (Layer layer : layers) {
            BufferedImage spriteImage = spriteToImage(layer.sprite);
            if (spriteImage == null) {
                continue;
            }
            BufferedImage scaled = scaleToIcon(spriteImage, width, height);
            composite(combined, scaled, layer.tint);
        }

        return combined;
    }

    private static List<Layer> extractLayers(IBakedModel model, ItemStack stack) {
        ItemColors itemColors = MC.getItemColors();
        List<Layer> ordered = new ArrayList<Layer>();
        Set<String> seen = new LinkedHashSet<String>();

        collectLayers(ordered, seen, model.getQuads(null, null, 0L), stack, itemColors);
        for (EnumFacing facing : EnumFacing.values()) {
            collectLayers(ordered, seen, model.getQuads(null, facing, 0L), stack, itemColors);
        }

        if (ordered.isEmpty() && model.getParticleTexture() != null) {
            ordered.add(new Layer(model.getParticleTexture(), 0xFFFFFFFF));
        }
        return ordered;
    }

    private static void collectLayers(List<Layer> ordered, Set<String> seen, List<BakedQuad> quads, ItemStack stack, ItemColors itemColors) {
        for (BakedQuad quad : quads) {
            if (quad == null || quad.getSprite() == null) {
                continue;
            }
            int tint = quad.hasTintIndex() ? itemColors.colorMultiplier(stack, quad.getTintIndex()) : 0xFFFFFFFF;
            if ((tint >>> 24) == 0) {
                tint |= 0xFF000000;
            }
            String key = quad.getSprite().getIconName() + "|" + Integer.toUnsignedString(tint);
            if (seen.add(key)) {
                ordered.add(new Layer(quad.getSprite(), tint));
            }
        }
    }

    @Nullable
    private static BufferedImage spriteToImage(TextureAtlasSprite sprite) {
        if (sprite.getFrameCount() <= 0) {
            return null;
        }
        int[][] frameData = sprite.getFrameTextureData(0);
        if (frameData == null || frameData.length == 0 || frameData[0] == null) {
            return null;
        }

        int width = sprite.getIconWidth();
        int height = sprite.getIconHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, frameData[0], 0, width);
        return image;
    }

    private static BufferedImage scaleToIcon(BufferedImage image, int width, int height) {
        if (image.getWidth() == width && image.getHeight() == height) {
            return image;
        }

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private static void composite(BufferedImage target, BufferedImage layer, int tint) {
        float tintA = ((tint >>> 24) & 0xFF) / 255.0f;
        int tintR = (tint >>> 16) & 0xFF;
        int tintG = (tint >>> 8) & 0xFF;
        int tintB = tint & 0xFF;

        for (int y = 0; y < target.getHeight(); y++) {
            for (int x = 0; x < target.getWidth(); x++) {
                int src = layer.getRGB(x, y);
                int srcA = (src >>> 24) & 0xFF;
                if (srcA == 0) {
                    continue;
                }

                float srcAlpha = (srcA / 255.0f) * tintA;
                int srcR = (((src >>> 16) & 0xFF) * tintR) / 255;
                int srcG = (((src >>> 8) & 0xFF) * tintG) / 255;
                int srcB = ((src & 0xFF) * tintB) / 255;

                int dst = target.getRGB(x, y);
                float dstAlpha = ((dst >>> 24) & 0xFF) / 255.0f;
                float outAlpha = srcAlpha + dstAlpha * (1.0f - srcAlpha);
                if (outAlpha <= 0.0f) {
                    target.setRGB(x, y, 0);
                    continue;
                }

                int dstR = (dst >>> 16) & 0xFF;
                int dstG = (dst >>> 8) & 0xFF;
                int dstB = dst & 0xFF;

                int outR = Math.min(255, Math.round((srcR * srcAlpha + dstR * dstAlpha * (1.0f - srcAlpha)) / outAlpha));
                int outG = Math.min(255, Math.round((srcG * srcAlpha + dstG * dstAlpha * (1.0f - srcAlpha)) / outAlpha));
                int outB = Math.min(255, Math.round((srcB * srcAlpha + dstB * dstAlpha * (1.0f - srcAlpha)) / outAlpha));
                int outA = Math.min(255, Math.round(outAlpha * 255.0f));

                target.setRGB(x, y, (outA << 24) | (outR << 16) | (outG << 8) | outB);
            }
        }
    }

    private static String makeKey(ItemStack stack) {
        StringBuilder builder = new StringBuilder();
        ResourceLocation registryName = stack.getItem().getRegistryName();
        builder.append(registryName == null ? "unknown" : registryName.toString());
        builder.append('#').append(stack.getMetadata());
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            builder.append('#').append(tag);
        }
        return builder.toString();
    }

    private static final class Layer {
        private final TextureAtlasSprite sprite;
        private final int tint;

        private Layer(TextureAtlasSprite sprite, int tint) {
            this.sprite = sprite;
            this.tint = tint;
        }
    }
}
