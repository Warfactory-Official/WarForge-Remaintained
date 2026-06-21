package com.flansmod.warforge.common.blocks.models;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeConfig;
import net.minecraft.resources.ResourceLocation;

/**
 * Statue model registry for claim/citadel/siege block-entity rendering.
 *
 * <p>The classic statues ({@code block/dummy/king|knight|berserker}) are already loaded by the
 * {@code dummy} blockstate variants; the modern statues ({@code block/statues/modern/*}) are not
 * referenced by any blockstate, so all five locations are registered as additional models on
 * {@link net.minecraftforge.client.event.ModelEvent.RegisterAdditional} (see
 * {@link com.flansmod.warforge.common.ModelEventHandler}). Forge then bakes them into the
 * {@link net.minecraft.client.resources.model.ModelManager}, from which
 * {@link com.flansmod.warforge.client.RenderTileEntityClaim} fetches them per-frame.
 */
public final class ClaimModels {

    // Classic (medieval) statues — also referenced by the `dummy` blockstate variants.
    public static final ResourceLocation STATUE_KING = new ResourceLocation(Tags.MODID, "block/dummy/king");
    public static final ResourceLocation STATUE_KNIGHT = new ResourceLocation(Tags.MODID, "block/dummy/knight");
    public static final ResourceLocation STATUE_BERSERKER = new ResourceLocation(Tags.MODID, "block/dummy/berserker");

    // Modern statues — not referenced by any blockstate, must be registered as additional models.
    public static final ResourceLocation MODERN_FLAG_POLE = new ResourceLocation(Tags.MODID, "block/statues/modern/flag_pole");
    public static final ResourceLocation MODERN_RADIO = new ResourceLocation(Tags.MODID, "block/statues/modern/radio");

    /** All locations Forge must bake; passed to {@code ModelEvent.RegisterAdditional}. */
    public static final ResourceLocation[] ADDITIONAL_MODELS = {
            STATUE_KING, STATUE_KNIGHT, STATUE_BERSERKER, MODERN_FLAG_POLE, MODERN_RADIO,
    };

    private ClaimModels() {
    }

    /**
     * Resolves the statue model location for a renderer type, honouring the runtime
     * {@link WarForgeConfig#MODERN_WARFARE_MODELS} theme toggle. There is no modern siege statue,
     * so siege always uses the classic berserker (matching the original behaviour).
     */
    public static ResourceLocation modelFor(ModelType type) {
        boolean modern = WarForgeConfig.MODERN_WARFARE_MODELS;
        return switch (type) {
            case CITADEL -> modern ? MODERN_FLAG_POLE : STATUE_KING;
            case BASIC_CLAIM -> modern ? MODERN_RADIO : STATUE_KNIGHT;
            case SIEGE -> STATUE_BERSERKER;
        };
    }

    public enum ModelType {
        CITADEL,
        BASIC_CLAIM,
        SIEGE,
    }
}
