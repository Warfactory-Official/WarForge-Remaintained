package com.flansmod.warforge.client;

import com.flansmod.warforge.client.render.RenderDistances;
import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.blocks.FobBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public class RenderTileEntityFob implements BlockEntityRenderer<FobBlockEntity> {

    public RenderTileEntityFob() {
    }

    @Override
    public void render(FobBlockEntity te, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (te.ownerFaction.equals(FactionIds.NONE)) return;
        PoleFlagRenderer.render(pose, buffers, packedOverlay, te.getLevel(), te.getBlockPos(),
                0, te.factionFlagId, te.getPoleLength(), partialTicks);
    }

    @Override
    public boolean shouldRenderOffScreen(FobBlockEntity te) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return RenderDistances.UNLIMITED;
    }
}
