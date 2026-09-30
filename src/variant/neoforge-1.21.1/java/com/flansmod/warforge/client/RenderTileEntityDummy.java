package com.flansmod.warforge.client;

import com.flansmod.warforge.client.render.BeamGeometry;
import com.flansmod.warforge.client.render.RenderBounds;
import com.flansmod.warforge.common.blocks.TileEntityDummy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

public class RenderTileEntityDummy implements BlockEntityRenderer<TileEntityDummy> {

    public RenderTileEntityDummy(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileEntityDummy te, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BeamGeometry.Beam beam = BeamGeometry.of(te);
        if (beam == null) return;

        long time = te.getLevel().getGameTime();
        pose.pushPose();
        pose.translate(0.0D, beam.start(), 0.0D); // X/Z centering done inside BeaconRenderer.renderBeaconBeam
        BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, partialTicks, 1.0F, time, 0,
                beam.height(), beam.packedColor(), 0.2F, 0.25F);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TileEntityDummy be) {
        return RenderBounds.of(be);
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityDummy te) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
