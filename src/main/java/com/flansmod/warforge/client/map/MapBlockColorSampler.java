package com.flansmod.warforge.client.map;

import com.flansmod.warforge.compat.Vanilla;

import com.flansmod.warforge.client.render.ModelSprites;

import com.flansmod.warforge.client.render.Images;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.registries.BuiltInRegistries;

import com.flansmod.warforge.common.WarForgeMod;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.flansmod.warforge.compat.Ids;

/**
 * Sampled like JourneyMap 1.20.1 (VanillaBlockSpriteProxy / ColorManager / VanillaBlockColorProxy). Render thread only.
 * Foliage x0.8, non-water fluids x fluid tint; grass/foliage/water use biome colors without neighbour blending.
 * No usable texture => vanilla map color.
 */
public final class MapBlockColorSampler {

    private static final ConcurrentHashMap<BlockState, Integer> BASE_COLOR_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> ICON_COLOR_CACHE = new ConcurrentHashMap<>();
    private static final int NO_COLOR = Integer.MIN_VALUE;
    private static final Set<Block> FAILED_BLOCKS = ConcurrentHashMap.newKeySet();

    private MapBlockColorSampler() {
    }

    public static int sampleColor(Level world, BlockState state, BlockPos pos) {
        try {
            return applyTint(world, state, pos, deriveBaseColor(state));
        } catch (RuntimeException e) {
            if (FAILED_BLOCKS.add(state.getBlock())) {
                WarForgeMod.LOGGER.warn("Map color sampling failed for {}; using vanilla map color", state.getBlock(), e);
            }
            return state.getMapColor(world, pos).col & 0x00FFFFFF;
        }
    }

    private static int deriveBaseColor(BlockState state) {
        int sprite = getBaseColor(state);
        if (sprite != NO_COLOR) {
            return sprite;
        }
        // Liquids whose still texture could not be resolved use JourneyMap's default fluid grey.
        if (state.getBlock() instanceof LiquidBlock) {
            return DEFAULT_FLUID_COLOR;
        }
        return materialColor(state);
    }

    private static int getBaseColor(BlockState state) {
        Integer cached = BASE_COLOR_CACHE.get(state);
        if (cached != null) {
            return cached;
        }
        int color = averageColor(getSprites(state));
        BASE_COLOR_CACHE.put(state, color);
        return color;
    }

    private static Collection<TextureAtlasSprite> getSprites(BlockState state) {
        Minecraft mc = Minecraft.getInstance();

        // Liquids have no model quads; sample the fluid's still texture directly.
        if (state.getBlock() instanceof LiquidBlock) {
            TextureAtlasSprite still = fluidStillSprite(mc, state);
            return still != null ? Collections.singletonList(still) : Collections.emptyList();
        }

        // Double plants only carry their distinctive texture on the upper half.
        if (state.hasProperty(DoublePlantBlock.HALF)) {
            state = state.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        }
        if (state.getRenderShape() != RenderShape.MODEL) {
            return Collections.emptyList();
        }

        return ModelSprites.blockSprites(state, MapBlockColorSampler::isUsable);
    }

    private static TextureAtlasSprite fluidStillSprite(Minecraft mc, BlockState state) {
        FluidState fluidState = state.getFluidState();
        if (fluidState.isEmpty()) {
            return null;
        }
        TextureAtlasSprite sprite = ModelSprites.fluidStillSprite(fluidState);
        return sprite != null && isUsable(sprite) ? sprite : null;
    }

    private static int averageColor(Collection<TextureAtlasSprite> sprites) {
        if (sprites == null || sprites.isEmpty()) {
            return NO_COLOR;
        }
        String key = iconKey(sprites);
        Integer cached = ICON_COLOR_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        long r = 0L;
        long g = 0L;
        long b = 0L;
        long count = 0L;
        for (TextureAtlasSprite sprite : sprites) {
            NativeImage atlasImage = spriteImage(sprite);
            NativeImage image = atlasImage != null ? atlasImage : loadSpritePng(sprite);
            if (image == null) {
                continue;
            }
            boolean owned = atlasImage == null; // PNG-loaded images are ours to free
            try {
                int width = image.getWidth();
                int height = image.getHeight();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int abgr = Images.getPixelABGR(image, x, y);
                        if (((abgr >>> 24) & 0xFF) > 0) {
                            r += abgr & 0xFF;
                            g += (abgr >> 8) & 0xFF;
                            b += (abgr >> 16) & 0xFF;
                            count++;
                        }
                    }
                }
            } finally {
                if (owned) {
                    image.close();
                }
            }
        }

        int result = count == 0L
                ? NO_COLOR
                : (((int) (r / count) << 16) | ((int) (g / count) << 8) | (int) (b / count));
        ICON_COLOR_CACHE.put(key, result);
        return result;
    }

    private static String iconKey(Collection<TextureAtlasSprite> sprites) {
        List<String> names = new ArrayList<>(sprites.size());
        for (TextureAtlasSprite sprite : sprites) {
            names.add(sprite.contents().name().toString());
        }
        Collections.sort(names);
        return String.join(",", names);
    }

    private static NativeImage spriteImage(TextureAtlasSprite sprite) {
        NativeImage image = sprite.contents().getOriginalImage();
        return (image != null && image.getWidth() > 0) ? image : null;
    }

    /** Reads the sprite's source PNG when its atlas image is gone, mirroring JourneyMap's fallback. */
    private static NativeImage loadSpritePng(TextureAtlasSprite sprite) {
        ResourceLocation icon = sprite.contents().name();
        ResourceLocation file = Ids.of(icon.getNamespace(), "textures/" + icon.getPath() + ".png");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream in = resource.get().open()) {
            return NativeImage.read(in);
        } catch (IOException e) {
            WarForgeMod.LOGGER.warn("Could not read sprite PNG {}", file, e);
            return null;
        }
    }

    private static int applyTint(Level world, BlockState state, BlockPos pos, int base) {
        if (isFoliage(state)) {
            return multiply(adjustBrightness(base, 0.8f), colorMultiplier(world, state, pos));
        }
        if (isFluid(state) && !isWater(state)) {
            return multiply(base, fluidTint(state));
        }
        return multiply(base, colorMultiplier(world, state, pos));
    }

    private static int colorMultiplier(Level world, BlockState state, BlockPos pos) {
        if (isGrass(state)) {
            return world.getBiome(pos).value().getGrassColor(pos.getX(), pos.getZ()) & 0x00FFFFFF;
        }
        if (isFoliage(state)) {
            return world.getBiome(pos).value().getFoliageColor() & 0x00FFFFFF;
        }
        if (isWater(state)) {
            return world.getBiome(pos).value().getWaterColor() & 0x00FFFFFF;
        }
        int color = ModelSprites.blockTint(state, world, pos);
        return color == -1 ? 0xFFFFFF : (color & 0x00FFFFFF);
    }

    private static int fluidTint(BlockState state) {
        FluidState fluidState = state.getFluidState();
        if (fluidState.isEmpty()) {
            return 0xFFFFFF;
        }
        return ModelSprites.fluidTint(fluidState) & 0x00FFFFFF;
    }

    private static boolean isGrass(BlockState state) {
        Block block = state.getBlock();
        return block instanceof GrassBlock || block == Vanilla.shortGrass();
    }

    private static boolean isFoliage(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock || block instanceof VineBlock) {
            return true;
        }
        return BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase(Locale.ROOT).contains("leaves");
    }

    private static boolean isFluid(BlockState state) {
        return state.getBlock() instanceof LiquidBlock;
    }

    private static boolean isWater(BlockState state) {
        return state.getBlock() instanceof LiquidBlock && state.getFluidState().is(FluidTags.WATER);
    }

    private static int materialColor(BlockState state) {
        return state.getBlock().defaultMapColor().col & 0x00FFFFFF;
    }

    private static boolean isUsable(TextureAtlasSprite sprite) {
        return sprite != null
                && sprite.contents() != null
                && !MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
    }

    private static int multiply(int c1, int c2) {
        int r = Math.round(((c1 >> 16) & 0xFF) * (((c2 >> 16) & 0xFF) / 255.0F));
        int g = Math.round(((c1 >> 8) & 0xFF) * (((c2 >> 8) & 0xFF) / 255.0F));
        int b = Math.round((c1 & 0xFF) * ((c2 & 0xFF) / 255.0F));
        return (r << 16) | (g << 8) | b;
    }

    private static int adjustBrightness(int color, float factor) {
        int r = clamp(Math.round(((color >> 16) & 0xFF) * factor));
        int g = clamp(Math.round(((color >> 8) & 0xFF) * factor));
        int b = clamp(Math.round((color & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value) {
        return value < 0 ? 0 : Math.min(value, 255);
    }

    private static final int DEFAULT_FLUID_COLOR = 0x00BCBCBC;
}
