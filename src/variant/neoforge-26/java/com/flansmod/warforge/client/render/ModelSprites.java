package com.flansmod.warforge.client.render;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
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
     * The distinct sprites of a block's model: the quads facing up (else the unculled quads), else the particle
     * sprite. Only sprites accepted by usable are returned.
     */
    @SuppressWarnings("deprecation")
    public static Collection<TextureAtlasSprite> blockSprites(BlockState state, Predicate<TextureAtlasSprite> usable) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        try {
            model.collectParts(RandomSource.create(), parts);
        } catch (Throwable ignored) {
            parts.clear();
        }

        Map<String, TextureAtlasSprite> sprites = new LinkedHashMap<>();
        for (Direction facing : new Direction[]{Direction.UP, null}) {
            if (collectSprites(parts, facing, sprites, usable)) {
                break;
            }
        }

        if (sprites.isEmpty()) {
            TextureAtlasSprite particle = model.particleMaterial().sprite();
            if (usable.test(particle)) {
                sprites.put(particle.contents().name().toString(), particle);
            }
        }
        return sprites.values();
    }

    private static boolean collectSprites(List<BlockStateModelPart> parts, @Nullable Direction facing,
                                          Map<String, TextureAtlasSprite> out, Predicate<TextureAtlasSprite> usable) {
        boolean added = false;
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(facing)) {
                TextureAtlasSprite sprite = quad.materialInfo().sprite();
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

    /** Records the layers that the item model resolver adds (the render state keeps them private). */
    private static final class LayerCapture extends ItemStackRenderState {
        private final List<LayerRenderState> layers = new ArrayList<>();

        @Override
        public LayerRenderState newLayer() {
            LayerRenderState layer = super.newLayer();
            layers.add(layer);
            return layer;
        }
    }

    /**
     * The distinct (sprite, tint) layers of an item's model in draw order, else its particle sprite untinted.
     */
    public static List<TintedSprite> itemLayers(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        LayerCapture capture = new LayerCapture();
        mc.getItemModelResolver().updateForTopItem(capture, stack, ItemDisplayContext.GUI, mc.level, null, 0);
        List<TintedSprite> ordered = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (ItemStackRenderState.LayerRenderState layer : capture.layers) {
            IntList tints = layer.tintLayers();
            for (BakedQuad quad : layer.prepareQuadList()) {
                if (quad == null) {
                    continue;
                }
                TextureAtlasSprite sprite = quad.materialInfo().sprite();
                int tintIndex = quad.materialInfo().tintIndex();
                int tint = tintIndex >= 0 && tintIndex < tints.size() ? tints.getInt(tintIndex) : 0xFFFFFFFF;
                if ((tint >>> 24) == 0) {
                    tint |= 0xFF000000;
                }
                String key = sprite.contents().name().toString() + "|" + Integer.toUnsignedString(tint);
                if (seen.add(key)) {
                    ordered.add(new TintedSprite(sprite, tint));
                }
            }
        }

        if (ordered.isEmpty()) {
            var particle = capture.pickParticleMaterial(RandomSource.create());
            if (particle != null) {
                ordered.add(new TintedSprite(particle.sprite(), 0xFFFFFFFF));
            }
        }
        return ordered;
    }

    /** The still texture sprite of a fluid, or null. */
    public static TextureAtlasSprite fluidStillSprite(FluidState fluidState) {
        return fluidModel(fluidState).stillMaterial().sprite();
    }

    /** The tint colour (ARGB) of a fluid. */
    public static int fluidTint(FluidState fluidState) {
        FluidTintSource source = fluidModel(fluidState).fluidTintSource();
        return source == null ? 0xFFFFFFFF : source.color(fluidState);
    }

    /** The block colour (tint index 0) at a position, or -1 for none. */
    public static int blockTint(BlockState state, Level level, BlockPos pos) {
        BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, 0);
        if (source == null) {
            return -1;
        }
        // 26.x Level is no BlockAndTintGetter any more; the client level (the only caller) still is.
        return level instanceof BlockAndTintGetter tintGetter ? source.colorInWorld(state, tintGetter, pos) : source.color(state);
    }

    private static FluidModel fluidModel(FluidState fluidState) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
    }
}
