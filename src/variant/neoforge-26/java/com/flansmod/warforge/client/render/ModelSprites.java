package com.flansmod.warforge.client.render;

import com.flansmod.warforge.common.WarForgeMod;
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

public final class ModelSprites {
    private static final Direction[] SPRITE_FACES = {Direction.UP, null};
    private static boolean modelFailureLogged;

    private static void logModelFailure(BlockState state, RuntimeException e) {
        if (!modelFailureLogged) {
            modelFailureLogged = true;
            WarForgeMod.LOGGER.debug("Block model of {} threw while sampling sprites; further failures not logged", state, e);
        }
    }

    private ModelSprites() {}

    public record TintedSprite(TextureAtlasSprite sprite, int tint) {
    }

    /** Up-facing quads, else unculled quads, else particle sprite; only sprites accepted by usable. */
    @SuppressWarnings("deprecation")
    public static Collection<TextureAtlasSprite> blockSprites(BlockState state, Predicate<TextureAtlasSprite> usable) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        try {
            model.collectParts(RandomSource.create(), parts);
        } catch (RuntimeException e) {
            logModelFailure(state, e);
            parts.clear();
        }

        Map<String, TextureAtlasSprite> sprites = new LinkedHashMap<>();
        for (Direction facing : SPRITE_FACES) {
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

    private static final class LayerCapture extends ItemStackRenderState {
        private final List<LayerRenderState> layers = new ArrayList<>();

        @Override
        public LayerRenderState newLayer() {
            LayerRenderState layer = super.newLayer();
            layers.add(layer);
            return layer;
        }
    }

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

    public static TextureAtlasSprite fluidStillSprite(FluidState fluidState) {
        return fluidModel(fluidState).stillMaterial().sprite();
    }

    public static int fluidTint(FluidState fluidState) {
        FluidTintSource source = fluidModel(fluidState).fluidTintSource();
        return source == null ? 0xFFFFFFFF : source.color(fluidState);
    }

    public static int blockTint(BlockState state, Level level, BlockPos pos) {
        BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, 0);
        if (source == null) {
            return -1;
        }
        // 26.x Level is not a BlockAndTintGetter; the client level (sole caller) is
        return level instanceof BlockAndTintGetter tintGetter ? source.colorInWorld(state, tintGetter, pos) : source.color(state);
    }

    private static FluidModel fluidModel(FluidState fluidState) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
    }
}
