package com.flansmod.warforge.api.modularui;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Samples per-block map colors the way JourneyMap does, instead of the flat ~64-entry vanilla
 * {@link net.minecraft.block.material.MapColor} palette.
 *
 * <p>Mirrors JourneyMap's {@code VanillaBlockColorProxy}:
 * <ol>
 *     <li><b>Base color</b> &mdash; the average RGB of the block's texture-sprite texels (only
 *     texels with alpha &gt; 0), taken across every sprite in the block's baked model. This is
 *     expensive, so it is computed once per {@link IBlockState} and cached.</li>
 *     <li><b>Tint</b> &mdash; grass / foliage / water use the biome's colors directly, everything
 *     else falls back to vanilla {@link net.minecraft.client.renderer.color.BlockColors}.</li>
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
    private static final ConcurrentHashMap<IBlockState, Integer> BASE_COLOR_CACHE = new ConcurrentHashMap<IBlockState, Integer>();
    private static final int NO_COLOR = -1;

    private MapBlockColorSampler() {
    }

    /**
     * Final, biome-tinted map color (0xRRGGBB) for {@code state} at {@code pos}. Never throws.
     */
    public static int sampleColor(World world, IBlockState state, BlockPos pos) {
        try {
            int base = getBaseColor(state);
            if (base != NO_COLOR) {
                Block block = state.getBlock();
                if (isFoliage(block)) {
                    return multiply(adjustBrightness(base, 0.8f), getTint(world, state, pos));
                }
                if (block instanceof IFluidBlock) {
                    Fluid fluid = ((IFluidBlock) block).getFluid();
                    if (fluid != null) {
                        return multiply(base, fluid.getColor());
                    }
                }
                return multiply(base, getTint(world, state, pos));
            }
        } catch (Throwable ignored) {
            // Fall through to the vanilla map color below.
        }
        return fallbackMapColor(world, state, pos);
    }

    private static int getBaseColor(IBlockState state) {
        Integer cached = BASE_COLOR_CACHE.get(state);
        if (cached != null) {
            return cached;
        }
        int color = computeSpriteAverage(state);
        BASE_COLOR_CACHE.put(state, color);
        return color;
    }

    private static int computeSpriteAverage(IBlockState state) {
        Minecraft mc = Minecraft.getMinecraft();

        // Liquids have no usable baked-model quads: their model resolves to the magenta/black
        // missing texture, which is what turned water purple. Sample the fluid's still texture
        // directly instead - covers modded Forge fluids (by registry) and vanilla water/lava
        // (by material), exactly like JourneyMap does for IFluidBlock.
        TextureAtlasSprite fluidSprite = fluidStillSprite(mc, state);
        if (fluidSprite != null) {
            return averageSprites(Collections.singletonList(fluidSprite));
        }

        if (state.getRenderType() == EnumBlockRenderType.INVISIBLE) {
            return NO_COLOR;
        }
        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        if (dispatcher == null) {
            return NO_COLOR;
        }
        IBakedModel model = dispatcher.getModelForState(state);
        if (model == null) {
            return NO_COLOR;
        }

        // Sample the UP face only - this is a top-down map. Averaging every face mixes in the
        // dark side/bottom textures, which darkened and muddied the tone. Fall back to the general
        // (cull-less) quads for cross models like plants, then to the particle texture.
        Set<TextureAtlasSprite> sprites = new LinkedHashSet<TextureAtlasSprite>();
        try {
            collectSprites(model.getQuads(state, EnumFacing.UP, 0L), sprites);
            if (sprites.isEmpty()) {
                collectSprites(model.getQuads(state, null, 0L), sprites);
            }
        } catch (Throwable ignored) {
            // Some modded models throw on off-thread queries; fall back to the particle texture.
        }
        if (sprites.isEmpty()) {
            TextureAtlasSprite particle = model.getParticleTexture();
            if (isUsable(particle)) {
                sprites.add(particle);
            }
        }
        return averageSprites(sprites);
    }

    /** Resolves the still texture for any liquid: modded Forge fluids, plus vanilla water/lava. */
    private static TextureAtlasSprite fluidStillSprite(Minecraft mc, IBlockState state) {
        String spriteName = null;
        Fluid fluid = FluidRegistry.lookupFluidForBlock(state.getBlock());
        if (fluid != null) {
            ResourceLocation still = fluid.getStill();
            if (still != null) {
                spriteName = still.toString();
            }
        }
        if (spriteName == null) {
            Material material = state.getMaterial();
            if (material == Material.WATER) {
                spriteName = "minecraft:blocks/water_still";
            } else if (material == Material.LAVA) {
                spriteName = "minecraft:blocks/lava_still";
            }
        }
        if (spriteName == null) {
            return null;
        }
        TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(spriteName);
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
            if (sprite == null || sprite.getFrameCount() <= 0) {
                continue;
            }
            int[][] frames;
            try {
                frames = sprite.getFrameTextureData(0);
            } catch (Throwable ignored) {
                continue;
            }
            if (frames == null || frames.length == 0 || frames[0] == null) {
                continue;
            }
            // MC stores frame data as ARGB (0xAARRGGBB), matching BufferedImage#getRGB.
            for (int pixel : frames[0]) {
                int alpha = (pixel >>> 24) & 0xFF;
                if (alpha > 0) {
                    r += (pixel >> 16) & 0xFF;
                    g += (pixel >> 8) & 0xFF;
                    b += pixel & 0xFF;
                    count++;
                }
            }
        }
        if (count == 0L) {
            return NO_COLOR;
        }
        return ((int) (r / count) << 16) | ((int) (g / count) << 8) | (int) (b / count);
    }

    /** Filters out null and the magenta/black missing-texture sprite. */
    private static boolean isUsable(TextureAtlasSprite sprite) {
        return sprite != null
                && sprite.getIconName() != null
                && !"missingno".equals(sprite.getIconName());
    }

    /** Biome tint multiplier (0xRRGGBB), mirroring JourneyMap's getColorMultiplier. */
    private static int getTint(World world, IBlockState state, BlockPos pos) {
        Block block = state.getBlock();
        Biome biome = world.getBiome(pos);
        if (isGrass(block, state)) {
            return biome.getGrassColorAtPos(pos);
        }
        if (isFoliage(block)) {
            return biome.getFoliageColorAtPos(pos);
        }
        if (isWater(state)) {
            return biome.getWaterColorMultiplier();
        }
        return Minecraft.getMinecraft().getBlockColors()
                .colorMultiplier(state, world, pos, block.getRenderLayer().ordinal());
    }

    private static int fallbackMapColor(World world, IBlockState state, BlockPos pos) {
        try {
            return state.getMapColor(world, pos).colorValue;
        } catch (Throwable ignored) {
            return 0x000000;
        }
    }

    private static boolean isGrass(Block block, IBlockState state) {
        return block instanceof BlockGrass || state.getMaterial() == Material.GRASS;
    }

    private static boolean isFoliage(Block block) {
        return block instanceof BlockLeaves || block instanceof BlockVine;
    }

    private static boolean isWater(IBlockState state) {
        return state.getMaterial() == Material.WATER;
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
        return value < 0 ? 0 : (value > 255 ? 255 : value);
    }
}
