package com.flansmod.warforge.client;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.LeaderboardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class TileEntityLeaderboardRenderer implements BlockEntityRenderer<LeaderboardBlockEntity>
{
    private final Font font;

    private static final String[] RANKS = new String[LeaderboardBlockEntity.NUM_ENTRIES];

    static {
        for (int i = 0; i < RANKS.length; i++) {
            RANKS[i] = "#" + (i + 1);
        }
    }

    private record Label(Component text, int width) {}

    private final Map<String, Label> labels = new HashMap<>();

    private Label label(String text)
    {
        Label label = labels.get(text);
        if (label == null) {
            if (labels.size() > 256) {
                labels.clear();
            }
            label = new Label(Component.literal(text), font.width(text));
            labels.put(text, label);
        }
        return label;
    }

    public TileEntityLeaderboardRenderer(BlockEntityRendererProvider.Context context)
    {
        this.font = context.getFont();
    }

    @Override
    public void render(LeaderboardBlockEntity te, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay)
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

        String type = "Bah";
        if(te.getBlockState().getBlock() == WarForgeMod.CONTENT.topLeaderboardBlock)
            type = "Top";
        else if(te.getBlockState().getBlock() == WarForgeMod.CONTENT.wealthLeaderboardBlock)
            type = "Wealth";
        else if(te.getBlockState().getBlock() == WarForgeMod.CONTENT.notorietyLeaderboardBlock)
            type = "Notoriety";
        else if(te.getBlockState().getBlock() == WarForgeMod.CONTENT.legacyLeaderboardBlock)
            type = "Legacy";

        Label typeLabel = label(type);
        drawString(pose, buffers, packedLight, typeLabel, fontArea / 2 - typeLabel.width() / 2, 0, true);
        Label title = label("Leaderboard");
        drawString(pose, buffers, packedLight, title, fontArea / 2 - title.width() / 2, 8, true);

        for(int i = 0; i < LeaderboardBlockEntity.NUM_ENTRIES - 1; i++)
        {
            drawString(pose, buffers, packedLight, label(RANKS[i]), 0, 20 + 10 * i, false);
            Label name = label(te.topNames[i] != null ? te.topNames[i] : "");
            drawString(pose, buffers, packedLight, name, fontArea - name.width(), 20 + 10 * i, false);
        }

        pose.popPose();
    }

    private void drawString(PoseStack pose, MultiBufferSource buffers, int packedLight, Label label, float x, float y, boolean shadow)
    {
        font.drawInBatch(label.text(), x, y, 0xffffff, shadow, pose.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, packedLight);
    }
}
