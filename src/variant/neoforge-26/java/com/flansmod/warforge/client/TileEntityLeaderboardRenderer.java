package com.flansmod.warforge.client;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.LeaderboardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TileEntityLeaderboardRenderer implements BlockEntityRenderer<LeaderboardBlockEntity, TileEntityLeaderboardRenderer.State>
{
    public static class State extends BlockEntityRenderState
    {
        public String type = "Bah";
        public String[] topNames = new String[0];
    }

    private final Font font;

    private static final String[] RANKS = new String[LeaderboardBlockEntity.NUM_ENTRIES];

    static {
        for (int i = 0; i < RANKS.length; i++) {
            RANKS[i] = "#" + (i + 1);
        }
    }

    private record Label(FormattedCharSequence text, int width) {}

    private final Map<String, Label> labels = new HashMap<>();

    private Label label(String text)
    {
        Label label = labels.get(text);
        if (label == null) {
            if (labels.size() > 256) {
                labels.clear();
            }
            label = new Label(Component.literal(text).getVisualOrderText(), font.width(text));
            labels.put(text, label);
        }
        return label;
    }

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
    public void extractRenderState(LeaderboardBlockEntity te, State state, float partialTicks, Vec3 cameraPosition,
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
        state.topNames = te.topNames;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
    {
        pose.pushPose();
        pose.translate(0.0F, 1.0F, 1.0F);
        pose.scale(1.0F, -1.0F, -1.0F);

        float mcScale = 1f / 16f;
        pose.scale(mcScale, mcScale, mcScale);

        pose.translate(3, 3, 6.25f);
        int fontArea = 13 - 3;

        float fontPxPerMcPx = 8;
        fontArea *= fontPxPerMcPx;
        float textScale = 1f / fontPxPerMcPx;
        pose.scale(textScale, textScale, textScale);

        String type = state.type;
        Label typeLabel = label(type);
        drawString(pose, collector, state.lightCoords, typeLabel, fontArea / 2 - typeLabel.width() / 2, 0, true);
        Label title = label("Leaderboard");
        drawString(pose, collector, state.lightCoords, title, fontArea / 2 - title.width() / 2, 8, true);

        for(int i = 0; i < LeaderboardBlockEntity.NUM_ENTRIES - 1; i++)
        {
            drawString(pose, collector, state.lightCoords, label(RANKS[i]), 0, 20 + 10 * i, false);
            Label name = label(i < state.topNames.length && state.topNames[i] != null ? state.topNames[i] : "");
            drawString(pose, collector, state.lightCoords, name, fontArea - name.width(), 20 + 10 * i, false);
        }

        pose.popPose();
    }

    private void drawString(PoseStack pose, SubmitNodeCollector collector, int packedLight, Label label, float x, float y, boolean shadow)
    {
        // alpha 0 drew opaque before 1.21.6; 26.x needs explicit alpha.
        collector.submitText(pose, x, y, label.text(), shadow, Font.DisplayMode.NORMAL,
                packedLight, 0xFFFFFFFF, 0, 0);
    }
}
