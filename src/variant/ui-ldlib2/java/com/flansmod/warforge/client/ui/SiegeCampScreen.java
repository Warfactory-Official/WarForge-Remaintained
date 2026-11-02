package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.client.ui.model.DataRevision;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.client.map.ChunkMapViewport;
import com.flansmod.warforge.api.modularui.MapCellElement;
import com.flansmod.warforge.client.ui.model.MomentumTooltip;
import com.flansmod.warforge.client.ui.model.SiegeMapModel;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.client.ui.model.SiegeCampAttackInfoRender;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

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
        if (data != null && REVISION.unchanged(ClientCompat.screen(), data)) {
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

        UIElement root = screenRoot();
        UIElement panel = panel(width, height);
        root.addChild(panel);

        headerBackdrop(panel, width);
        UIElement infoSection = section(panel, CONTENT_LEFT, infoY, mapSectionWidth, INFO_HEIGHT);
        place(panel, box(mapSectionWidth, mapSectionHeight, sectionBackdrop()), CONTENT_LEFT, mapY, mapSectionWidth, mapSectionHeight);
        place(panel, box(mapSize, mapSize, insetBackdrop(MAP_BACKDROP_FILL)), mapLeft, mapTop, mapSize, mapSize);
        UIElement legendSection = section(panel, CONTENT_LEFT, legendY, mapSectionWidth, LEGEND_HEIGHT);
        colorStripe(panel, data.color, height);
        closeButton(panel, width).setOnClick(event -> ClientCompat.setScreen(null));

        title(panel, tr("warforge.ui.siege_target_map"), TEXT_PRIMARY);
        place(panel, scrollingText(literal(model.campLine(), TEXT_SECONDARY, false), width - CONTENT_LEFT * 2 - 18),
                CONTENT_LEFT, SUBTITLE_Y, width - CONTENT_LEFT * 2 - 18, 9);

        Label momentum = scrollingText(literal(tr("warforge.ui.current_momentum_d", data.momentum), TEXT_PRIMARY, false), mapSectionWidth - 10);
        momentum.layout(l -> l.marginBottom(4));
        infoSection.addChild(tooltip(momentum, MomentumTooltip.lines().toArray(new Component[0])));
        infoSection.addChild(scrollingText(literal(tr("warforge.ui.select_a_target_chunk_around_the_siege_camp_cent"),
                TEXT_MUTED, false), mapSectionWidth - 10));
        legendSection.addChild(scrollingText(literal(tr("warforge.ui.click_an_attackable_chunk_to_begin_the_siege_too"),
                TEXT_MUTED, false), mapSectionWidth - 10));

        for (int j = viewport.startZ; j < viewport.startZ + viewport.visibleSize; j++) {
            for (int i = viewport.startX; i < viewport.startX + viewport.visibleSize; i++) {
                int index = j * totalSize + i;
                int x = mapLeft + (i - viewport.startX) * CELL_SIZE;
                int y = mapTop + (j - viewport.startZ) * CELL_SIZE;
                SiegeCampAttackInfo rawInfo = model.orderedAttacks.get(index);
                if (rawInfo == null) {
                    continue;
                }

                SiegeCampAttackInfoRender chunkInfo = model.renderInfo(rawInfo);
                MapCellElement cell = new MapCellElement(model.textureName(chunkInfo), chunkInfo, model.adjacency[index]);
                cell.onClick = button -> model.attack(chunkInfo);
                dynamicTooltip(cell, () -> model.tooltip(chunkInfo));
                place(panel, cell, x, y, CELL_SIZE, CELL_SIZE);
            }
        }

        ModularUIScreen built = new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal(tr("warforge.ui.siege_target_map")));
        REVISION.shown(built);
        ClientCompat.setScreen(built);
    }

}
