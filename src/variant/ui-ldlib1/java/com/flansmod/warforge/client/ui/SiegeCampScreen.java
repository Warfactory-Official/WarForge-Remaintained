package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.client.ui.model.DataRevision;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.client.map.ChunkMapViewport;
import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.MapCellElement;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.ui.model.MomentumTooltip;
import com.flansmod.warforge.client.ui.model.SiegeMapModel;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.client.ui.model.SiegeCampAttackInfoRender;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;
import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.HEADER_HEIGHT;

@OnlyIn(Dist.CLIENT)
public final class SiegeCampScreen {
    private static final DataRevision REVISION = new DataRevision();
    private static final int CELL_SIZE = 64;
    private static final int INFO_HEIGHT = 48;
    private static final int LEGEND_HEIGHT = 28;
    private static final int MAP_GUTTER = 18;
    private static final int SECTION_SPACING = 8;

    private SiegeCampScreen() {
    }

    public static void open(PacketSiegeCampInfo data) {
        if (data != null && REVISION.unchanged(Minecraft.getInstance().screen, data)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int totalSize = 2 * SiegeMapModel.RADIUS + 1;
        ChunkMapViewport viewport = ChunkMapViewport.create(
                totalSize,
                3,
                totalSize,
                CELL_SIZE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                48,
                72,
                -1,
                -1
        );

        SiegeMapModel model = new SiegeMapModel(data);

        int mapSize = CELL_SIZE * viewport.visibleSize;
        int mapSectionWidth = mapSize + MAP_GUTTER * 2;
        int mapSectionHeight = mapSize + MAP_GUTTER * 2;
        int width = mapSectionWidth + CONTENT_LEFT * 2;
        int infoY = HEADER_HEIGHT + SECTION_SPACING;
        int mapY = infoY + INFO_HEIGHT + SECTION_SPACING;
        int legendY = mapY + mapSectionHeight + SECTION_SPACING;
        int height = legendY + LEGEND_HEIGHT + 12;
        int mapLeft = CONTENT_LEFT + MAP_GUTTER;
        int mapTop = mapY + MAP_GUTTER;
        int textWidth = mapSectionWidth - 10;

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, width, height);
        panel.addWidget(WarForgeUiTheme.header(width));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, infoY, mapSectionWidth, INFO_HEIGHT));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, mapY, mapSectionWidth, mapSectionHeight));
        panel.addWidget(WarForgeUiTheme.box(mapLeft, mapTop, mapSize, mapSize, WarForgeUiTheme.insetBackdrop(WarForgeUiTheme.MAP_BACKDROP_FILL)));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, legendY, mapSectionWidth, LEGEND_HEIGHT));
        panel.addWidget(WarForgeUiTheme.stripe(data.color, height));
        panel.addWidget(WarForgeUiTheme.closeButton(width, click -> ClientScreens.close()));

        panel.addWidget(WarForgeUiTheme.title(tr("warforge.ui.siege_target_map")));
        panel.addWidget(Label.text(model.campLine(), WarForgeUiTheme.TEXT_SECONDARY)
                .clip(width - CONTENT_LEFT * 2 - 18).at(CONTENT_LEFT, WarForgeUiTheme.SUBTITLE_Y));

        int infoTextY = infoY + WarForgeUiTheme.SECTION_PADDING;
        Label momentum = Label.text(tr("warforge.ui.current_momentum_d", data.momentum), WarForgeUiTheme.TEXT_PRIMARY)
                .clip(textWidth).at(CONTENT_LEFT + 5, infoTextY);
        momentum.setHoverTooltips(MomentumTooltip.lines());
        panel.addWidget(momentum);
        panel.addWidget(Label.text(tr("warforge.ui.select_a_target_chunk_around_the_siege_camp_cent"),
                WarForgeUiTheme.TEXT_MUTED).clip(textWidth).at(CONTENT_LEFT + 5, infoTextY + 9 + 4));
        panel.addWidget(Label.text(tr("warforge.ui.click_an_attackable_chunk_to_begin_the_siege_too"),
                WarForgeUiTheme.TEXT_MUTED).clip(textWidth).at(CONTENT_LEFT + 5, legendY + WarForgeUiTheme.SECTION_PADDING));

        for (int j = viewport.startZ; j < viewport.startZ + viewport.visibleSize; j++) {
            for (int i = viewport.startX; i < viewport.startX + viewport.visibleSize; i++) {
                int index = j * totalSize + i;
                SiegeCampAttackInfo rawInfo = model.orderedAttacks.get(index);
                if (rawInfo == null) {
                    continue;
                }

                SiegeCampAttackInfoRender chunkInfo = model.renderInfo(rawInfo);

                MapCellElement cell = new MapCellElement(model.textureName(chunkInfo), chunkInfo, model.adjacency[index], CELL_SIZE);
                cell.setSelfPosition(mapLeft + (i - viewport.startX) * CELL_SIZE, mapTop + (j - viewport.startZ) * CELL_SIZE);
                cell.onClick = button -> model.attack(chunkInfo);
                cell.tooltipOnHover(() -> styledTooltip(model.tooltip(chunkInfo), chunkInfo));
                panel.addWidget(cell);
            }
        }

        REVISION.shown(ClientScreens.open(panel));
    }

    private static boolean isAttackTime(Component line) {
        return line.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("warforge.client.siege_map.attack_time");
    }

    private static List<Component> styledTooltip(List<Component> lines, SiegeCampAttackInfoRender chunkInfo) {
        List<Component> styled = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            if (i == 0) {
                styled.add(chunkInfo.factionName.isEmpty()
                        ? line.copy().withStyle(ChatFormatting.GREEN)
                        : line.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(chunkInfo.factionColour & 0xFFFFFF))));
            } else if (chunkInfo.canAttack && i == lines.size() - 1) {
                styled.add(line.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
            } else if (chunkInfo.canAttack && isAttackTime(line)) {
                styled.add(line.copy().withStyle(ChatFormatting.RED));
            } else {
                styled.add(line);
            }
        }
        return styled;
    }
}
