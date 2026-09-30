package com.flansmod.warforge.client;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.render.FlagGeometry;
import com.flansmod.warforge.compat.Ids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Draws the claim pole model and its waving flag (geometry in FlagGeometry). */
public final class PoleFlagRenderer {

    /** The additional pole model (registered in ModelEventHandler). */
    public static final ResourceLocation POLE_MODEL = Ids.of(Tags.MODID, "block/pole");

    private PoleFlagRenderer() {
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, int packedOverlay,
                              Level level, BlockPos pos, int rotation, String flagId,
                              float poleLength, float partialTicks) {
        BakedModel bakedModel = Minecraft.getInstance().getModelManager().getModel(POLE_MODEL);
        BlockState dummyState = Blocks.STONE.defaultBlockState();

        int poleLight = LevelRenderer.getLightColor(level, pos.above());

        pose.pushPose();
        pose.translate(0.5D, 1.0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation * 45.0F));
        pose.scale(FlagGeometry.MODEL_SCALE, FlagGeometry.MODEL_SCALE, FlagGeometry.MODEL_SCALE);

        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());
        PoseStack.Pose basePose = pose.last();

        pose.pushPose();
        pose.translate(0.0D, FlagGeometry.SHAFT_BASE_Y, 0.0D);
        pose.scale(1.0F, FlagGeometry.shaftStretch(poleLength), 1.0F);
        pose.translate(0.0D, -FlagGeometry.SHAFT_BASE_Y, 0.0D);
        PoseStack.Pose shaftPose = pose.last();

        RandomSource random = RandomSource.create();
        for (Direction dir : Direction.values()) {
            for (BakedQuad quad : bakedModel.getQuads(dummyState, dir, random)) {
                consumer.putBulkData(isShaftQuad(quad) ? shaftPose : basePose, quad, 1.0F, 1.0F, 1.0F, poleLight, packedOverlay);
            }
        }
        for (BakedQuad quad : bakedModel.getQuads(dummyState, null, random)) {
            consumer.putBulkData(isShaftQuad(quad) ? shaftPose : basePose, quad, 1.0F, 1.0F, 1.0F, poleLight, packedOverlay);
        }
        pose.popPose();

        renderFlag(pose, buffers, level, poleLight, packedOverlay, flagId, FlagGeometry.bannerTop(poleLength), partialTicks);

        pose.popPose();
    }

    private static boolean isShaftQuad(BakedQuad quad) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        for (int i = 0; i < 4; i++) {
            if (FlagGeometry.isShaftY(Float.intBitsToFloat(vertices[i * stride + 1]))) {
                return true;
            }
        }
        return false;
    }

    private static void renderFlag(PoseStack pose, MultiBufferSource buffers, Level level,
                                   int packedLight, int packedOverlay, String flagId,
                                   float bannerTop, float partialTicks) {
        ResourceLocation flagTexture = ClientFlagRegistry.getFlagTexture(flagId);
        if (flagTexture == null) return;

        float time = FlagGeometry.waveTime(level.getGameTime(), partialTicks);
        VertexConsumer c = buffers.getBuffer(RenderType.entityCutout(flagTexture));
        PoseStack.Pose p = pose.last();
        FlagGeometry.emitFlag(ClientFlagRegistry.getFlagDimensions(flagId), bannerTop, time, (x, y, z, u, v, nz) ->
                c.vertex(p.pose(), x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(packedOverlay)
                        .uv2(packedLight).normal(p.normal(), 0.0F, 0.0F, nz).endVertex());
    }
}
