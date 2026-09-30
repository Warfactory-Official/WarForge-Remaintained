package com.flansmod.warforge.client;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityLeaderboard;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TileEntityLeaderboardRenderer implements BlockEntityRenderer<TileEntityLeaderboard, TileEntityLeaderboardRenderer.State>
{
	/** The leaderboard text, captured during extraction. */
	public static class State extends BlockEntityRenderState
	{
		public String type = "Bah";
		public String[] topNames = new String[0];
	}

	private final Font font;

	public TileEntityLeaderboardRenderer(BlockEntityRendererProvider.Context context)
	{
		this.font = context.font();
	}

	@Override
	public State createRenderState()
	{
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityLeaderboard te, State state, float partialTicks, Vec3 cameraPosition,
	                               ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
	{
		BlockEntityRenderState.extractBase(te, state, breakProgress);
		Block block = te.getBlockState().getBlock();
		String type = "Bah";
		if(block == WarForgeMod.CONTENT.topLeaderboardBlock)
			type = "Top";
		else if(block == WarForgeMod.CONTENT.wealthLeaderboardBlock)
			type = "Wealth";
		else if(block == WarForgeMod.CONTENT.notorietyLeaderboardBlock)
			type = "Notoriety";
		else if(block == WarForgeMod.CONTENT.legacyLeaderboardBlock)
			type = "Legacy";
		state.type = type;
		state.topNames = te.topNames.clone();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
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

		String type = state.type;
		drawString(pose, collector, state.lightCoords, type, fontArea / 2 - font.width(type) / 2, 0, true);
		drawString(pose, collector, state.lightCoords, "Leaderboard", fontArea / 2 - font.width("Leaderboard") / 2, 8, true);

		for(int i = 0; i < TileEntityLeaderboard.NUM_ENTRIES - 1; i++)
		{
			drawString(pose, collector, state.lightCoords, "#" + (i + 1), 0, 20 + 10 * i, false);
			String name = i < state.topNames.length && state.topNames[i] != null ? state.topNames[i] : "";
			drawString(pose, collector, state.lightCoords, name, fontArea - font.width(name), 20 + 10 * i, false);
		}

		pose.popPose();
	}

	private void drawString(PoseStack pose, SubmitNodeCollector collector, int packedLight, String text, float x, float y, boolean shadow)
	{
		// 0xffffff was drawn opaque before 1.21.6 (an alpha of 0 meant opaque); 26.x needs the alpha.
		collector.submitText(pose, x, y, Component.literal(text).getVisualOrderText(), shadow, Font.DisplayMode.NORMAL,
				packedLight, 0xFFFFFFFF, 0, 0);
	}
}
