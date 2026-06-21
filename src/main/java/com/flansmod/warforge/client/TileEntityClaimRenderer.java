package com.flansmod.warforge.client;

import com.flansmod.warforge.common.blocks.TileEntityClaim;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class TileEntityClaimRenderer implements BlockEntityRenderer<TileEntityClaim>
{
	public TileEntityClaimRenderer(BlockEntityRendererProvider.Context context)
	{
	}

	@Override
	public void render(TileEntityClaim te, float partialTicks, PoseStack pose, MultiBufferSource buffers,
	                   int packedLight, int packedOverlay)
	{
		//List<String> flags = te.getPlayerFlags();

		pose.pushPose();
		pose.translate(0.0F, 1.0F, 0.0F);

		float mcScale = 1f;
		pose.scale(mcScale, mcScale, mcScale);

		// x=3 to x=13
		// y=3 to y=13
		//pose.translate(3, 3, 6.25f);
		//int fontArea = 13 - 3;

		// Font allowed area is now 0-80 font px
		//float fontPxPerMcPx = 8;
		//fontArea *= fontPxPerMcPx;
		//float textScale = 1f / fontPxPerMcPx;
		//pose.scale(textScale, textScale, textScale);

		double poleH = /*flags.size()*/ 1 * 1.2d + 1.2d;

		// TODO: flag-pole / waving-flag geometry not yet implemented

		pose.popPose();
	}
}
