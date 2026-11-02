package com.flansmod.warforge.client;

import com.flansmod.warforge.client.render.RenderDistances;
import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.client.render.RenderBounds;
import com.flansmod.warforge.common.blocks.ClaimBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;

public class RenderTileEntityClaim implements BlockEntityRenderer<ClaimBlockEntity> {

    public RenderTileEntityClaim() {
    }

    @Override
    public void render(ClaimBlockEntity te, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (te.getFaction().equals(FactionIds.NONE)) return;
        PoleFlagRenderer.render(pose, buffers, packedOverlay, te.getLevel(), te.getBlockPos(),
                te.rotation, te.factionFlagId, te.getPoleLength(), partialTicks);
    }

    @Override
    public boolean shouldRenderOffScreen(ClaimBlockEntity te) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return RenderDistances.UNLIMITED;
    }

    @Override
    public AABB getRenderBoundingBox(ClaimBlockEntity be) {
        return RenderBounds.of(be);
    }
}
