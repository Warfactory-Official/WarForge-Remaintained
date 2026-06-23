package com.flansmod.warforge.client;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.blocks.TileEntityClaim;
import com.flansmod.warforge.common.blocks.models.ClaimModels;
import com.flansmod.warforge.server.Faction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderTileEntityClaim implements BlockEntityRenderer<TileEntityClaim> {

    // Square section of the procedural pole.
    private static final float POLE_HALF = 0.1F;

    // Waving animation: subdivide the banner horizontally and offset each column along Z by a sine.
    private static final int WAVE_SEGMENTS = 8;
    private static final float WAVE_AMPLITUDE = 0.12F;
    private static final float WAVE_SPEED = 0.12F;
    private static final float WAVE_FREQUENCY = 0.45F;

    public final ClaimModels.ModelType model;

    public RenderTileEntityClaim(ClaimModels.ModelType model) {
        this.model = model;
    }

    // Per-model/theme geometry so the same waving banner adapts to whichever statue/pole base is in
    // use (the modern theme swaps the base model). All values are in block units, pole centred at 0.5.
    private record FlagProfile(boolean drawPole, float poleHeight, float anchorX, float bannerTopY,
                               float maxBannerWidth, float maxBannerHeight) {
    }

    private FlagProfile profileFor() {
        boolean modern = WarForgeConfig.MODERN_WARFARE_MODELS;
        return switch (model) {
            case CITADEL -> modern
                    ? new FlagProfile(true, 2.6F, 0.5F - POLE_HALF, 2.4F, 1.05F, 0.66F)
                    : new FlagProfile(true, 2.2F, 0.9F - POLE_HALF, 2.5F, 0.9F, 0.6F);
            case BASIC_CLAIM -> new FlagProfile(true, 1.7F, 0.5F - POLE_HALF, 1.5F, 0.6F, 0.4F);
            case SIEGE -> new FlagProfile(true, 1.7F, 0.5F - POLE_HALF, 1.5F, 0.6F, 0.4F);
        };
    }

    @Override
    public void render(TileEntityClaim te, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (te.getFaction().equals(Faction.nullUuid)) return;

        // Fetch from the native bake every frame; getModel never returns null (it returns the
        // missing model if the location was not baked).
        BakedModel bakedModel = Minecraft.getInstance().getModelManager()
                .getModel(ClaimModels.modelFor(model));
        BlockState dummyState = Blocks.STONE.defaultBlockState();

        pose.pushPose();
        pose.translate(0.0D, 1.0D, 0.0D);
        // te.rotation is 0..7 (8 steps of 45 degrees), rotated about the block centre. This replaces
        // the 8 pre-baked rotated model variants of the old custom bakery.
        pose.translate(0.5D, 0.0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(te.rotation * 45.0F));
        pose.translate(-0.5D, 0.0D, -0.5D);

        // The passed packedLight samples the citadel BASE block, which is often enclosed/dark and
        // leaves the statue rendering black. The statue is drawn one block above the base
        // (the +1 Y translate above / the `statue` placeholder position), so sample the light there.
        int statueLight = LevelRenderer.getLightColor(
                te.getLevel(), te.getBlockPos().above());

        ModelBlockRenderer modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        modelRenderer.renderModel(pose.last(), consumer, dummyState, bakedModel,
                1.0F, 1.0F, 1.0F, statueLight, packedOverlay, ModelData.EMPTY, RenderType.solid());

        renderFlag(te, partialTicks, pose, buffers, statueLight, packedOverlay);

        pose.popPose();
    }

    private void renderFlag(TileEntityClaim te, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                            int packedLight, int packedOverlay) {
        FlagProfile profile = profileFor();

        // Pole, centred on the block, drawn even when no flag texture has arrived yet.
        if (profile.drawPole()) {
            pose.pushPose();
            pose.translate(0.5D, 0.0D, 0.5D);
            renderPole(pose, buffers, packedLight, profile.poleHeight());
            pose.popPose();
        }

        String flagId = te.factionFlagId;
        ResourceLocation flagTexture = ClientFlagRegistry.getFlagTexture(flagId);
        if (flagTexture == null) return;

        // Fit the banner inside the profile's box while preserving the flag's true pixel aspect ratio,
        // so a 16:9 upload no longer renders as a square. Dimensions are null until a custom flag's
        // chunks finish streaming in; fall back to square until then.
        int[] dims = ClientFlagRegistry.getFlagDimensions(flagId);
        float aspect = (dims != null && dims.length == 2 && dims[1] > 0) ? (float) dims[0] / dims[1] : 1.0F;
        float boxAspect = profile.maxBannerWidth() / profile.maxBannerHeight();
        float bannerWidth;
        float bannerHeight;
        if (aspect >= boxAspect) {
            bannerWidth = profile.maxBannerWidth();
            bannerHeight = bannerWidth / aspect;
        } else {
            bannerHeight = profile.maxBannerHeight();
            bannerWidth = bannerHeight * aspect;
        }

        float bannerTop = profile.bannerTopY();
        float bannerBottom = bannerTop - bannerHeight;

        float time = WAVE_SPEED * ((float) (te.getLevel().getGameTime() % 100000L) + partialTicks);

        VertexConsumer flag = buffers.getBuffer(RenderType.entityCutout(flagTexture));
        Matrix4f mat = pose.last().pose();
        Matrix3f norm = pose.last().normal();

        // Banner hangs off the pole along +X, the hoist edge anchored at the pole.
        float x0 = profile.anchorX();
        float dx = bannerWidth / WAVE_SEGMENTS;

        for (int seg = 0; seg < WAVE_SEGMENTS; seg++) {
            float xa = x0 + dx * seg;
            float xb = x0 + dx * (seg + 1);
            float ua = (float) seg / WAVE_SEGMENTS;
            float ub = (float) (seg + 1) / WAVE_SEGMENTS;

            float za = wave(time, seg);
            float zb = wave(time, seg + 1);

            // Front face (+Z normal) and back face (-Z normal) so the flag is visible from both sides.
            quad(flag, mat, norm, xa, xb, za, zb, ua, ub, bannerTop, bannerBottom, packedLight, packedOverlay, true);
            quad(flag, mat, norm, xa, xb, za, zb, ua, ub, bannerTop, bannerBottom, packedLight, packedOverlay, false);
        }
    }

    private static float wave(float time, int seg) {
        return WAVE_AMPLITUDE * Mth.sin(time + seg * WAVE_FREQUENCY) * seg / WAVE_SEGMENTS;
    }

    private static void quad(VertexConsumer c, Matrix4f mat, Matrix3f norm,
                             float xa, float xb, float za, float zb, float ua, float ub,
                             float bannerTop, float bannerBottom,
                             int packedLight, int packedOverlay, boolean front) {
        float nz = front ? 1.0F : -1.0F;
        float z = front ? 0.005F : -0.005F;
        if (front) {
            c.vertex(mat, xa, bannerTop, za + z).color(255, 255, 255, 255).uv(ua, 0.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(norm, 0.0F, 0.0F, nz).endVertex();
            c.vertex(mat, xa, bannerBottom, za + z).color(255, 255, 255, 255).uv(ua, 1.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(norm, 0.0F, 0.0F, nz).endVertex();
            c.vertex(mat, xb, bannerBottom, zb + z).color(255, 255, 255, 255).uv(ub, 1.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(norm, 0.0F, 0.0F, nz).endVertex();
            c.vertex(mat, xb, bannerTop, zb + z).color(255, 255, 255, 255).uv(ub, 0.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(norm, 0.0F, 0.0F, nz).endVertex();
        } else {
            c.vertex(mat, xb, bannerTop, zb + z).color(255, 255, 255, 255).uv(ub, 0.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(norm, 0.0F, 0.0F, nz).endVertex();
            c.vertex(mat, xb, bannerBottom, zb + z).color(255, 255, 255, 255).uv(ub, 1.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(norm, 0.0F, 0.0F, nz).endVertex();
            c.vertex(mat, xa, bannerBottom, za + z).color(255, 255, 255, 255).uv(ua, 1.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(norm, 0.0F, 0.0F, nz).endVertex();
            c.vertex(mat, xa, bannerTop, za + z).color(255, 255, 255, 255).uv(ua, 0.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(norm, 0.0F, 0.0F, nz).endVertex();
        }
    }

    // Pole corners in CCW order around the square section.
    private static final float[] POLE_CX = {-POLE_HALF, POLE_HALF, POLE_HALF, -POLE_HALF};
    private static final float[] POLE_CZ = {POLE_HALF, POLE_HALF, -POLE_HALF, -POLE_HALF};

    private static void renderPole(PoseStack pose, MultiBufferSource buffers, int packedLight, float poleHeight) {
        // Untextured square-section pole. RenderType.leash() is the POSITION_COLOR_LIGHTMAP,
        // no-texture, lit, double-sided pipeline drawn as a TRIANGLE_STRIP, so we walk the four
        // side columns as a closed strip (corner 0,1,2,3,0) emitting a bottom/top pair each.
        VertexConsumer pole = buffers.getBuffer(RenderType.leash());
        Matrix4f mat = pose.last().pose();

        int r = 200, g = 200, b = 205;
        for (int i = 0; i <= POLE_CX.length; i++) {
            int c = i % POLE_CX.length;
            float x = POLE_CX[c];
            float z = POLE_CZ[c];
            pole.vertex(mat, x, 0.0F, z).color(r, g, b, 255).uv2(packedLight).endVertex();
            pole.vertex(mat, x, poleHeight, z).color(r, g, b, 255).uv2(packedLight).endVertex();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityClaim te) {
        return true;
    }
}
