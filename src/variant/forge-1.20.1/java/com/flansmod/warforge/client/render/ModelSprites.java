package com.flansmod.warforge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Texture sprites of block and item models. The model API differs between versions. */
public final class ModelSprites {
    private ModelSprites() {}

    /** A sprite of an item model with its tint colour (ARGB, 0xFFFFFFFF for none). */
    public record TintedSprite(TextureAtlasSprite sprite, int tint) {
    }

    /**
     * The distinct sprites of a block's model: the quads facing up (else all quads), of the state (else of any
     * state), else the particle sprite. Only sprites accepted by usable are returned.
     */
    public static Collection<TextureAtlasSprite> blockSprites(BlockState state, Predicate<TextureAtlasSprite> usable) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        if (model == null) {
            return Collections.emptyList();
        }

        Map<String, TextureAtlasSprite> sprites = new LinkedHashMap<>();
        outer:
        for (BlockState variant : new BlockState[]{state, null}) {
            for (Direction facing : new Direction[]{Direction.UP, null}) {
                if (collectSprites(model, variant, facing, sprites, usable)) {
                    break outer;
                }
            }
        }

        if (sprites.isEmpty()) {
            TextureAtlasSprite particle = model.getParticleIcon();
            if (usable.test(particle)) {
                sprites.put(particle.contents().name().toString(), particle);
            }
        }
        return sprites.values();
    }

    private static boolean collectSprites(BakedModel model, BlockState state, Direction facing,
                                          Map<String, TextureAtlasSprite> out, Predicate<TextureAtlasSprite> usable) {
        boolean added = false;
        RandomSource rand = RandomSource.create();
        for (RenderType type : RenderType.chunkBufferLayers()) {
            List<BakedQuad> quads;
            try {
                quads = model.getQuads(state, facing, rand, ModelData.EMPTY, type);
            } catch (Throwable ignored) {
                continue;
            }
            if (quads == null || quads.isEmpty()) {
                continue;
            }
            for (BakedQuad quad : quads) {
                TextureAtlasSprite sprite = quad.getSprite();
                if (!usable.test(sprite)) {
                    continue;
                }
                String name = sprite.contents().name().toString();
                if (out.putIfAbsent(name, sprite) == null) {
                    added = true;
                }
            }
        }
        return added;
    }

    /**
     * The distinct (sprite, tint) layers of an item's model in draw order, else its particle sprite untinted.
     */
    public static List<TintedSprite> itemLayers(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getItemRenderer().getModel(stack, null, null, 0);
        RandomSource rand = RandomSource.create();
        List<TintedSprite> ordered = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        collectLayers(ordered, seen, model.getQuads(null, null, rand), stack, mc);
        for (Direction facing : Direction.values()) {
            collectLayers(ordered, seen, model.getQuads(null, facing, rand), stack, mc);
        }

        TextureAtlasSprite particle = model.getParticleIcon();
        if (ordered.isEmpty() && particle != null) {
            ordered.add(new TintedSprite(particle, 0xFFFFFFFF));
        }
        return ordered;
    }

    private static void collectLayers(List<TintedSprite> ordered, Set<String> seen, List<BakedQuad> quads, ItemStack stack, Minecraft mc) {
        for (BakedQuad quad : quads) {
            if (quad == null || quad.getSprite() == null) {
                continue;
            }
            int tint = (quad.getTintIndex() != -1) ? mc.getItemColors().getColor(stack, quad.getTintIndex()) : 0xFFFFFFFF;
            if ((tint >>> 24) == 0) {
                tint |= 0xFF000000;
            }
            String key = quad.getSprite().contents().name().toString() + "|" + Integer.toUnsignedString(tint);
            if (seen.add(key)) {
                ordered.add(new TintedSprite(quad.getSprite(), tint));
            }
        }
    }

    /** The still texture sprite of a fluid, or null. */
    public static TextureAtlasSprite fluidStillSprite(FluidState fluidState) {
        ResourceLocation still = IClientFluidTypeExtensions.of(fluidState).getStillTexture();
        if (still == null) {
            return null;
        }
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
    }

    /** The tint colour (ARGB) of a fluid. */
    public static int fluidTint(FluidState fluidState) {
        return IClientFluidTypeExtensions.of(fluidState).getTintColor();
    }

    /** The block colour (tint index 0) at a position, or -1 for none. */
    public static int blockTint(BlockState state, Level level, BlockPos pos) {
        return Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
    }
}
