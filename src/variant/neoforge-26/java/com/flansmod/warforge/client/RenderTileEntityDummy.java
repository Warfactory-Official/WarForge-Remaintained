package com.flansmod.warforge.client;

import com.flansmod.warforge.client.render.BeamGeometry;
import com.flansmod.warforge.client.render.RenderBounds;
import com.flansmod.warforge.common.blocks.TileEntityDummy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RenderTileEntityDummy implements BlockEntityRenderer<TileEntityDummy, RenderTileEntityDummy.State> {

    /** The beam of the dummy block entity, captured during extraction. */
    public static class State extends BlockEntityRenderState {
        public BeamGeometry.@Nullable Beam beam;
        public float animationTime;
    }

    public RenderTileEntityDummy(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileEntityDummy te, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(te, state, breakProgress);
        state.beam = BeamGeometry.of(te);
        state.animationTime = Math.floorMod(te.getLevel().getGameTime(), 40) + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        BeamGeometry.Beam beam = state.beam;
        if (beam == null) return;

        pose.pushPose();
        pose.translate(0.0D, beam.start(), 0.0D); // X/Z centering done inside BeaconRenderer.submitBeaconBeam
        BeaconRenderer.submitBeaconBeam(pose, collector, BeaconRenderer.BEAM_LOCATION, 1.0F, state.animationTime, 0,
                beam.height(), beam.packedColor(), 0.2F, 0.25F);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TileEntityDummy be) {
        return RenderBounds.of(be);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
