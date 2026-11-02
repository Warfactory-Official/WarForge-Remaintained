package com.flansmod.warforge.client;

import com.flansmod.warforge.client.render.RenderDistances;
import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.blocks.FobBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RenderTileEntityFob implements BlockEntityRenderer<FobBlockEntity, PoleFlagRenderer.State> {
    public RenderTileEntityFob() {
    }

    @Override
    public PoleFlagRenderer.State createRenderState() {
        return new PoleFlagRenderer.State();
    }

    @Override
    public void extractRenderState(FobBlockEntity te, PoleFlagRenderer.State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(te, state, breakProgress);
        PoleFlagRenderer.extract(state, !te.ownerFaction.equals(FactionIds.NONE), te.getLevel(), te.getBlockPos(),
                0, te.factionFlagId, te.getPoleLength(), partialTicks);
    }

    @Override
    public void submit(PoleFlagRenderer.State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        PoleFlagRenderer.submit(state, pose, collector);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return RenderDistances.UNLIMITED;
    }
}
