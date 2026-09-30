package com.flansmod.warforge.client;

import com.flansmod.warforge.client.render.FlagGeometry;
import com.flansmod.warforge.compat.ClientCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jetbrains.annotations.Nullable;

/** Draws the claim pole model and its waving flag (geometry in FlagGeometry). */
public final class PoleFlagRenderer {

    /** The standalone pole model (registered in ModelEventHandler). */
    public static final StandaloneModelKey<QuadCollection> POLE_MODEL = new StandaloneModelKey<>(() -> "warforge:block/pole");

    private PoleFlagRenderer() {
    }

    /** What the pole renderer needs from the block entity, captured during extraction. */
    public static class State extends BlockEntityRenderState {
        public boolean visible;
        public int rotation;
        public @Nullable String flagId;
        public float poleLength;
        public int poleLight;
        public float waveTime;
    }

    /** Fills the state. rotation is in steps of 45 degrees. */
    public static void extract(State state, boolean visible, Level level, BlockPos pos, int rotation, String flagId,
                               float poleLength, float partialTicks) {
        state.visible = visible;
        if (!visible) {
            return;
        }
        state.rotation = rotation;
        state.flagId = flagId;
        state.poleLength = poleLength;
        state.poleLight = ClientCompat.lightCoords(level, pos.above());
        state.waveTime = FlagGeometry.waveTime(level.getGameTime(), partialTicks);
    }

    public static void submit(State state, PoseStack pose, SubmitNodeCollector collector) {
        if (!state.visible) {
            return;
        }
        QuadCollection model = Minecraft.getInstance().getModelManager().getStandaloneModel(POLE_MODEL);
        int packedOverlay = OverlayTexture.NO_OVERLAY;

        pose.pushPose();
        pose.translate(0.5D, 1.0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.rotation * 45.0F));
        pose.scale(FlagGeometry.MODEL_SCALE, FlagGeometry.MODEL_SCALE, FlagGeometry.MODEL_SCALE);

        PoseStack.Pose basePose = pose.last().copy();

        pose.pushPose();
        pose.translate(0.0D, FlagGeometry.SHAFT_BASE_Y, 0.0D);
        pose.scale(1.0F, FlagGeometry.shaftStretch(state.poleLength), 1.0F);
        pose.translate(0.0D, -FlagGeometry.SHAFT_BASE_Y, 0.0D);
        PoseStack.Pose shaftPose = pose.last().copy();
        pose.popPose();

        if (model != null) {
            QuadInstance instance = new QuadInstance();
            instance.setColor(-1);
            instance.setLightCoords(state.poleLight);
            instance.setOverlayCoords(packedOverlay);
            collector.submitCustomGeometry(pose, RenderTypes.cutoutMovingBlock(), (p, consumer) -> {
                for (BakedQuad quad : model.getAll()) {
                    consumer.putBakedQuad(isShaftQuad(quad) ? shaftPose : basePose, quad, instance);
                }
            });
        }

        submitFlag(state, pose, collector, packedOverlay, FlagGeometry.bannerTop(state.poleLength));

        pose.popPose();
    }

    private static boolean isShaftQuad(BakedQuad quad) {
        for (int i = 0; i < 4; i++) {
            if (FlagGeometry.isShaftY(quad.position(i).y())) {
                return true;
            }
        }
        return false;
    }

    private static void submitFlag(State state, PoseStack pose, SubmitNodeCollector collector, int packedOverlay, float bannerTop) {
        if (state.flagId == null) return;
        Identifier flagTexture = ClientFlagRegistry.getFlagTexture(state.flagId);
        if (flagTexture == null) return;

        int[] dimensions = ClientFlagRegistry.getFlagDimensions(state.flagId);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(flagTexture), (p, c) ->
                FlagGeometry.emitFlag(dimensions, bannerTop, state.waveTime, (x, y, z, u, v, nz) ->
                        c.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(packedOverlay)
                                .setLight(state.poleLight).setNormal(p, 0.0F, 0.0F, nz)));
    }
}
