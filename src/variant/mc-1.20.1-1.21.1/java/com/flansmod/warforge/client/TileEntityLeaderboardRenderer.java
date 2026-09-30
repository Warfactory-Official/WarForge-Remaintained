package com.flansmod.warforge.client;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityLeaderboard;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;

public class TileEntityLeaderboardRenderer implements BlockEntityRenderer<TileEntityLeaderboard>
{
	private final Font font;

	public TileEntityLeaderboardRenderer(BlockEntityRendererProvider.Context context)
	{
		this.font = context.getFont();
	}

	@Override
	public void render(TileEntityLeaderboard te, float partialTicks, PoseStack pose, MultiBufferSource buffers,
	                   int packedLight, int packedOverlay)
	{
		pose.pushPose();
		pose.translate(0.0F, 1.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);

		float mcScale = 1f / 16f;
		pose.scale(mcScale, mcScale, mcScale);

		// x=3 to x=13
		// y=3 to y=13
		pose.translate(3, 3, 6.25f);
		int fontArea = 13 - 3;

		// Font allowed area is now 0-80 font px
		float fontPxPerMcPx = 8;
		fontArea *= fontPxPerMcPx;
		float textScale = 1f / fontPxPerMcPx;
		pose.scale(textScale, textScale, textScale);

		String type = "Bah";
		if(te.getBlockState().getBlock() == WarForgeMod.CONTENT.topLeaderboardBlock)
			type = "Top";
		else if(te.getBlockState().getBlock() == WarForgeMod.CONTENT.wealthLeaderboardBlock)
			type = "Wealth";
		else if(te.getBlockState().getBlock() == WarForgeMod.CONTENT.notorietyLeaderboardBlock)
			type = "Notoriety";
		else if(te.getBlockState().getBlock() == WarForgeMod.CONTENT.legacyLeaderboardBlock)
			type = "Legacy";

		drawString(pose, buffers, packedLight, type, fontArea / 2 - font.width(type) / 2, 0, true);
		drawString(pose, buffers, packedLight, "Leaderboard", fontArea / 2 - font.width("Leaderboard") / 2, 8, true);

		for(int i = 0; i < TileEntityLeaderboard.NUM_ENTRIES - 1; i++)
		{
			drawString(pose, buffers, packedLight, "#" + (i + 1), 0, 20 + 10 * i, false);
			String name = te.topNames[i] != null ? te.topNames[i] : "";
			drawString(pose, buffers, packedLight, name, fontArea - font.width(name), 20 + 10 * i, false);
		}

		pose.popPose();
	}

	private void drawString(PoseStack pose, MultiBufferSource buffers, int packedLight, String text, float x, float y, boolean shadow)
	{
		font.drawInBatch(Component.literal(text), x, y, 0xffffff, shadow, pose.last().pose(), buffers,
				Font.DisplayMode.NORMAL, 0, packedLight);
	}
}
