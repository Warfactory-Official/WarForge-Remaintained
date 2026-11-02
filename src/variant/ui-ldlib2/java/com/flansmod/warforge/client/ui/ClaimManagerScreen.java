package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.client.ui.model.ClaimManagerView;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.client.map.ChunkMapViewport;
import com.flansmod.warforge.api.modularui.MapCellElement;
import com.flansmod.warforge.client.ui.model.ClaimMapModel;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

@OnlyIn(Dist.CLIENT)
public final class ClaimManagerScreen {
    private static final int CELL_SIZE = 64;
    private static final int MAX_VISIBLE_RADIUS = 4;
    private static final int INFO_HEIGHT = 30;
    private static final int LEGEND_HEIGHT = 20;
    private static final int MAP_GUTTER = 18;
    private static final int SECTION_SPACING = 4;
    private static final int NAV_BUTTON_SIZE = 16;

    private ClaimManagerScreen() {
    }

    public static boolean isCurrentlyOpen() {
        return ClaimManagerView.isOpen(ClientCompat.screen());
    }

    public static void refreshIfOpen() {
        ClaimManagerView.refreshIfOpen(ClientCompat.screen(), ClaimManagerScreen::open);
    }

    public static void open(DimChunkPos center, int radius, int pageX, int pageZ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int size = radius * 2 + 1;
        ChunkMapViewport viewport = ChunkMapViewport.create(
                size,
                3,
                Math.min(size, MAX_VISIBLE_RADIUS * 2 + 1),
                CELL_SIZE,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),
                CONTENT_LEFT * 2 + 40,
                64,
                pageX,
                pageZ
        );

        ClaimMapModel.requestTextures(center, radius);
        final ClaimMapModel.Modes modes = ClaimMapModel.resolveModes(center);

        int visible = viewport.visibleSize;
        ClaimMapModel.Cell[] cells = new ClaimMapModel.Cell[visible * visible];
        for (int j = 0; j < visible; j++) {
            for (int i = 0; i < visible; i++) {
                cells[j * visible + i] = ClaimMapModel.cell(center, radius, viewport.startX + i, viewport.startZ + j, modes);
            }
        }
        int signature = 31 * ClaimManagerView.signature(center, radius, pageX, pageZ, modes, cells) + ClaimMapModel.claimsLine().hashCode();
        if (ClaimManagerView.unchanged(ClientCompat.screen(), signature)) {
            return;
        }

        int mapSize = viewport.visibleSize * CELL_SIZE;
        int mapSectionWidth = mapSize + MAP_GUTTER * 2;
        int mapSectionHeight = mapSize + MAP_GUTTER * 2;
        int width = mapSectionWidth + CONTENT_LEFT * 2;
        int infoY = HEADER_HEIGHT + SECTION_SPACING;
        int mapY = infoY + INFO_HEIGHT + SECTION_SPACING;
        int legendY = mapY + mapSectionHeight + SECTION_SPACING;
        int height = legendY + LEGEND_HEIGHT + 12;
        int mapLeft = CONTENT_LEFT + MAP_GUTTER;
        int mapTop = mapY + MAP_GUTTER;

        ClaimMapModel.Cell centerCell = ClaimMapModel.cell(center, radius, radius, radius, modes);
        int accentColor = centerCell.info.factionId.equals(FactionIds.NONE) ? NO_FACTION_STRIPE : centerCell.info.colour;

        UIElement root = screenRoot();
        UIElement panel = panel(width, height);
        root.addChild(panel);

        headerBackdrop(panel, width);
        UIElement infoSection = section(panel, CONTENT_LEFT, infoY, mapSectionWidth, INFO_HEIGHT);
        place(panel, box(mapSectionWidth, mapSectionHeight, sectionBackdrop()), CONTENT_LEFT, mapY, mapSectionWidth, mapSectionHeight);
        place(panel, box(mapSize, mapSize, insetBackdrop(MAP_BACKDROP_FILL)), mapLeft, mapTop, mapSize, mapSize);
        UIElement legendSection = section(panel, CONTENT_LEFT, legendY, mapSectionWidth, LEGEND_HEIGHT);
        colorStripe(panel, accentColor, height);
        closeButton(panel, width).setOnClick(event -> ClientCompat.setScreen(null));

        title(panel, ClaimMapModel.title(modes), TEXT_PRIMARY);
        place(panel, scrollingText(literal(ClaimMapModel.centerLine(center, radius), TEXT_SECONDARY, false), width - CONTENT_LEFT * 2 - 18),
                CONTENT_LEFT, SUBTITLE_Y, width - CONTENT_LEFT * 2 - 18, 9);

        Label status = scrollingText(literal(ClaimMapModel.claimsLine(), TEXT_PRIMARY, false), mapSectionWidth - 10);
        status.layout(l -> l.marginBottom(4));
        infoSection.addChild(status);
        infoSection.addChild(scrollingText(literal(ClaimManagerView.pagingLine(viewport.visibleSize, size), TEXT_MUTED, false), mapSectionWidth - 10));
        legendSection.addChild(scrollingText(literal(ClaimMapModel.legend(modes), TEXT_MUTED, false), mapSectionWidth - 10));

        addPanButtons(panel, center, radius, viewport, CONTENT_LEFT, mapY, mapSectionWidth, mapSectionHeight);

        if (modes.canDeclareSiege && !modes.target && !modes.startPick) {
            place(panel, actionButton(tr("warforge.ui.declare_siege"), 84, () -> ClaimMapModel.declareSiege(center, radius, pageX, pageZ)),
                    width - 112, 9, 84, 16);
        } else if (modes.target) {
            place(panel, actionButton(tr("warforge.ui.cancel"), 60, () -> ClaimMapModel.cancelTarget(center, radius, pageX, pageZ)),
                    width - 86, 9, 60, 16);
        } else if (modes.startPick) {
            place(panel, actionButton(tr("warforge.ui.cancel"), 60, ClaimMapModel::cancelStartPick), width - 86, 9, 60, 16);
        }

        for (int j = viewport.startZ; j < viewport.startZ + viewport.visibleSize; j++) {
            for (int i = viewport.startX; i < viewport.startX + viewport.visibleSize; i++) {
                ClaimMapModel.Cell cellData = cells[(j - viewport.startZ) * visible + (i - viewport.startX)];
                int localX = i - viewport.startX;
                int localZ = j - viewport.startZ;

                MapCellElement cell = new MapCellElement(cellData.textureName, cellData.renderInfo, cellData.adjacency);
                cell.onClick = button -> ClaimMapModel.click(center, radius, cellData, modes, button);
                dynamicTooltip(cell, () -> ClaimMapModel.tooltip(center, cellData, modes));
                place(panel, cell, mapLeft + localX * CELL_SIZE, mapTop + localZ * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        ModularUIScreen screen = new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal(tr("warforge.ui.claim_manager")));
        ClaimManagerView.shown(screen, signature, center, radius, pageX, pageZ);
        ClientCompat.setScreen(screen);
    }

    private static void addPanButtons(UIElement panel, DimChunkPos center, int radius, ChunkMapViewport viewport,
                                      int sectionX, int sectionY, int sectionWidth, int sectionHeight) {
        if (viewport.canPanNorth()) {
            panButton(panel, "^", sectionX + sectionWidth / 2 - NAV_BUTTON_SIZE / 2, sectionY + 1,
                    center, radius, viewport.startX, viewport.panNorth());
        }
        if (viewport.canPanSouth()) {
            panButton(panel, "v", sectionX + sectionWidth / 2 - NAV_BUTTON_SIZE / 2, sectionY + sectionHeight - NAV_BUTTON_SIZE - 1,
                    center, radius, viewport.startX, viewport.panSouth());
        }
        if (viewport.canPanWest()) {
            panButton(panel, "<", sectionX + 1, sectionY + sectionHeight / 2 - NAV_BUTTON_SIZE / 2,
                    center, radius, viewport.panWest(), viewport.startZ);
        }
        if (viewport.canPanEast()) {
            panButton(panel, ">", sectionX + sectionWidth - NAV_BUTTON_SIZE - 1, sectionY + sectionHeight / 2 - NAV_BUTTON_SIZE / 2,
                    center, radius, viewport.panEast(), viewport.startZ);
        }
    }

    private static void panButton(UIElement panel, String text, int x, int y, DimChunkPos center, int radius, int pageX, int pageZ) {
        Button button = actionButton(text, NAV_BUTTON_SIZE, () -> UiNavigation.openClaimManager(center, radius, pageX, pageZ));
        button.layout(l -> l.paddingAll(0));
        button.text.layout(l -> l.marginHorizontal(0));
        place(panel, button, x, y, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE);
    }
}
