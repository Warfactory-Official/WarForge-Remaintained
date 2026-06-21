package com.flansmod.warforge.api.modularui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Samples per-block map colors the way JourneyMap does, instead of the flat ~64-entry vanilla
 * {@link MapColor} palette.
 *
 * <p>Mirrors JourneyMap's {@code VanillaBlockColorProxy}:
 * <ol>
 *     <li><b>Base color</b> &mdash; the average RGB of the block's texture-sprite texels (only
 *     texels with alpha &gt; 0), taken across every sprite in the block's baked model. This is
 *     expensive, so it is computed once per {@link BlockState} and cached.</li>
 *     <li><b>Tint</b> &mdash; grass / foliage / water use the biome's colors directly, everything
 *     else falls back to vanilla {@link BlockColors}.</li>
 *     <li>Foliage base colors are darkened (&times;0.8) and fluids are multiplied by their fluid
 *     color, just like JourneyMap.</li>
 * </ol>
 *
 * <p>Anything that has no usable texture (invisible blocks, sprites whose frame data was released)
 * falls back to the vanilla map color so the map never gets holes. All sampling is wrapped so a
 * misbehaving modded model degrades to the vanilla color rather than killing the map thread.
 */
public final class MapBlockColorSampler {

    /** Untinted sprite-average color per block state, in 0xRRGGBB, or {@link #NO_COLOR} if none. */
    private static final ConcurrentHashMap<BlockState, Integer> BASE_COLOR_CACHE = new ConcurrentHashMap<>();
    private static final int NO_COLOR = -1;

    private MapBlockColorSampler() {
    }

    /**
     * Final, biome-tinted map color (0xRRGGBB) for {@code state} at {@code pos}. Never throws.
     */
    public static int sampleColor(Level world, BlockState state, BlockPos pos) {
        try {
            int base = getBaseColor(state);
            if (base != NO_COLOR) {
                Block block = state.getBlock();
                if (isFoliage(block)) {
                    return multiply(adjustBrightness(base, 0.8f), getTint(world, state, pos));
                }
                FluidState fluidState = state.getFluidState();
                if (!fluidState.isEmpty()) {
                    int fluidColor = IClientFluidTypeExtensions.of(fluidState).getTintColor();
                    return multiply(base, fluidColor & 0x00FFFFFF);
                }
                return multiply(base, getTint(world, state, pos));
            }
        } catch (Throwable ignored) {
            // Fall through to the vanilla map color below.
        }
        return fallbackMapColor(world, state, pos);
    }

    private static int getBaseColor(BlockState state) {
        Integer cached = BASE_COLOR_CACHE.get(state);
        if (cached != null) {
            return cached;
        }
        int color = computeSpriteAverage(state);
        BASE_COLOR_CACHE.put(state, color);
        return color;
    }

    private static int computeSpriteAverage(BlockState state) {
        Minecraft mc = Minecraft.getInstance();

        // Liquids have no usable baked-model quads: their model resolves to the missing texture.
        // Sample the fluid's still texture directly instead, exactly like JourneyMap does for fluids.
        TextureAtlasSprite fluidSprite = fluidStillSprite(mc, state);
        if (fluidSprite != null) {
            return averageSprites(Collections.singletonList(fluidSprite));
        }

        if (state.getRenderShape() == RenderShape.INVISIBLE) {
            return NO_COLOR;
        }
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        if (dispatcher == null) {
            return NO_COLOR;
        }
        BakedModel model = dispatcher.getBlockModel(state);
        if (model == null) {
            return NO_COLOR;
        }

        // Sample the UP face only - this is a top-down map. Averaging every face mixes in the
        // dark side/bottom textures, which darkened and muddied the tone. Fall back to the general
        // (cull-less) quads for cross models like plants, then to the particle texture.
        Set<TextureAtlasSprite> sprites = new LinkedHashSet<>();
        RandomSource rand = RandomSource.create();
        try {
            collectSprites(model.getQuads(state, Direction.UP, rand, ModelData.EMPTY, null), sprites);
            if (sprites.isEmpty()) {
                collectSprites(model.getQuads(state, null, rand, ModelData.EMPTY, null), sprites);
            }
        } catch (Throwable ignored) {
            // Some modded models throw on off-thread queries; fall back to the particle texture.
        }
        if (sprites.isEmpty()) {
            TextureAtlasSprite particle = model.getParticleIcon();
            if (isUsable(particle)) {
                sprites.add(particle);
            }
        }
        return averageSprites(sprites);
    }

    /** Resolves the still texture for any liquid: modded Forge fluids, plus vanilla water/lava. */
    private static TextureAtlasSprite fluidStillSprite(Minecraft mc, BlockState state) {
        FluidState fluidState = state.getFluidState();
        if (fluidState.isEmpty()) {
            return null;
        }
        ResourceLocation still = IClientFluidTypeExtensions.of(fluidState).getStillTexture();
        if (still == null) {
            return null;
        }
        TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        return isUsable(sprite) ? sprite : null;
    }

    private static void collectSprites(List<BakedQuad> quads, Set<TextureAtlasSprite> out) {
        if (quads == null) {
            return;
        }
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.getSprite();
            if (isUsable(sprite)) {
                out.add(sprite);
            }
        }
    }

    /** Mean RGB over every alpha &gt; 0 texel of the given sprites, or {@link #NO_COLOR}. */
    private static int averageSprites(Collection<TextureAtlasSprite> sprites) {
        long r = 0L;
        long g = 0L;
        long b = 0L;
        long count = 0L;
        for (TextureAtlasSprite sprite : sprites) {
            if (sprite == null) {
                continue;
            }
            SpriteContents contents = sprite.contents();
            int width = contents.width();
            int height = contents.height();
            NativeImage image = contents.getOriginalImage();
            if (width <= 0 || height <= 0 || image == null) {
                continue;
            }
            // NativeImage stores pixels as ABGR (0xAABBGGRR).
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int alpha = (abgr >>> 24) & 0xFF;
                    if (alpha > 0) {
                        r += abgr & 0xFF;
                        g += (abgr >> 8) & 0xFF;
                        b += (abgr >> 16) & 0xFF;
                        count++;
                    }
                }
            }
        }
        if (count == 0L) {
            return NO_COLOR;
        }
        return ((int) (r / count) << 16) | ((int) (g / count) << 8) | (int) (b / count);
    }

    /** Filters out null and the missing-texture sprite. */
    private static boolean isUsable(TextureAtlasSprite sprite) {
        return sprite != null
                && sprite.contents() != null
                && !MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
    }

    /** Biome tint multiplier (0xRRGGBB), mirroring JourneyMap's getColorMultiplier. */
    private static int getTint(Level world, BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (isGrass(block)) {
            return BiomeColors.getAverageGrassColor(world, pos);
        }
        if (isFoliage(block)) {
            return BiomeColors.getAverageFoliageColor(world, pos);
        }
        if (!state.getFluidState().isEmpty()) {
            return BiomeColors.getAverageWaterColor(world, pos);
        }
        int color = Minecraft.getInstance().getBlockColors().getColor(state, world, pos, 0);
        return color == -1 ? 0xFFFFFF : (color & 0x00FFFFFF);
    }

    private static int fallbackMapColor(Level world, BlockState state, BlockPos pos) {
        try {
            return state.getMapColor(world, pos).col & 0x00FFFFFF;
        } catch (Throwable ignored) {
            return 0x000000;
        }
    }

    private static boolean isGrass(Block block) {
        return block instanceof GrassBlock;
    }

    private static boolean isFoliage(Block block) {
        return block instanceof LeavesBlock || block instanceof VineBlock;
    }

    /** Per-channel color multiply in normalized space (out = c1 * c2 / 255), ignoring alpha. */
    private static int multiply(int c1, int c2) {
        int r = Math.round(((c1 >> 16) & 0xFF) * (((c2 >> 16) & 0xFF) / 255.0F));
        int g = Math.round(((c1 >> 8) & 0xFF) * (((c2 >> 8) & 0xFF) / 255.0F));
        int b = Math.round((c1 & 0xFF) * ((c2 & 0xFF) / 255.0F));
        return (r << 16) | (g << 8) | b;
    }

    /** Scale every channel by {@code factor}, clamped to [0, 255]. */
    private static int adjustBrightness(int color, float factor) {
        int r = clamp(Math.round(((color >> 16) & 0xFF) * factor));
        int g = clamp(Math.round(((color >> 8) & 0xFF) * factor));
        int b = clamp(Math.round((color & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value) {
        return value < 0 ? 0 : (Math.min(value, 255));
    }
}
