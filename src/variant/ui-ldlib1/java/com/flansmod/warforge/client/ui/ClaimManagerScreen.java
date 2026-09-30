package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.api.modularui.ChunkMapViewport;
import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.MapCellElement;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.ClientClaimChunkCache;
import com.flansmod.warforge.client.ui.model.ClaimMapModel;
import com.flansmod.warforge.common.factories.ClaimManagerGuiFactory;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;
import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.HEADER_HEIGHT;

/** The territory map (layout of the 1.20.1 GuiClaimManager). */
@OnlyIn(Dist.CLIENT)
public final class ClaimManagerScreen {
    private static final int CELL_SIZE = 64;
    private static final int MAX_VISIBLE_RADIUS = 4;
    private static final int INFO_HEIGHT = 30;
    private static final int LEGEND_HEIGHT = 20;
    private static final int MAP_GUTTER = 18;
    private static final int SECTION_SPACING = 4;
    private static final int NAV_BUTTON_SIZE = 16;

    private static ModularUIGuiContainer activeScreen = null;
    private static DimChunkPos lastCenter = null;
    private static int lastRadius = 4;
    private static int lastPageX = -1;
    private static int lastPageZ = -1;

    private ClaimManagerScreen() {
    }

    public static boolean isCurrentlyOpen() {
        Minecraft mc = Minecraft.getInstance();
        return activeScreen != null && mc.screen == activeScreen;
    }

    public static void refreshIfOpen() {
        if (!isCurrentlyOpen() || lastCenter == null) {
            return;
        }
        open(lastCenter, lastRadius, lastPageX, lastPageZ);
    }

    public static void open(DimChunkPos center, int radius, int pageX, int pageZ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        lastCenter = center;
        lastRadius = radius;
        lastPageX = pageX;
        lastPageZ = pageZ;

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
        int textWidth = mapSectionWidth - 10;

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, width, height);
        panel.addWidget(WarForgeUiTheme.header(width));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, infoY, mapSectionWidth, INFO_HEIGHT));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, mapY, mapSectionWidth, mapSectionHeight));
        panel.addWidget(WarForgeUiTheme.box(mapLeft, mapTop, mapSize, mapSize, WarForgeUiTheme.insetBackdrop(WarForgeUiTheme.MAP_BACKDROP_FILL)));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, legendY, mapSectionWidth, LEGEND_HEIGHT));
        panel.addWidget(WarForgeUiTheme.stripe(accentColor(center), height));
        panel.addWidget(WarForgeUiTheme.closeButton(width, click -> ClientScreens.close()));

        panel.addWidget(WarForgeUiTheme.title(ClaimMapModel.title(modes)));
        panel.addWidget(Label.text(ClaimMapModel.centerLine(center, radius), WarForgeUiTheme.TEXT_SECONDARY)
                .clip(width - CONTENT_LEFT * 2 - 18).at(CONTENT_LEFT, WarForgeUiTheme.SUBTITLE_Y));

        int infoTextY = infoY + WarForgeUiTheme.SECTION_PADDING;
        panel.addWidget(Label.text(ClaimMapModel.claimsLine(), WarForgeUiTheme.TEXT_PRIMARY)
                .clip(textWidth).at(CONTENT_LEFT + 5, infoTextY));
        panel.addWidget(Label.text(viewport.visibleSize < size
                        ? "Paged view " + viewport.visibleSize + "x" + viewport.visibleSize + " across the selected radius."
                        : "Showing the full selected radius.", WarForgeUiTheme.TEXT_MUTED)
                .clip(textWidth).at(CONTENT_LEFT + 5, infoTextY + 9 + 4));
        panel.addWidget(Label.text(legend(modes), WarForgeUiTheme.TEXT_MUTED)
                .clip(textWidth).at(CONTENT_LEFT + 5, legendY + WarForgeUiTheme.SECTION_PADDING));

        addPanButtons(panel, center, radius, viewport, CONTENT_LEFT, mapY, mapSectionWidth, mapSectionHeight);

        if (modes.canDeclareSiege && !modes.target && !modes.startPick) {
            panel.addWidget(headerButton("Declare Siege", 84, width - 112,
                    () -> ClaimMapModel.declareSiege(center, radius, pageX, pageZ)));
        } else if (modes.target) {
            panel.addWidget(headerButton("Cancel", 60, width - 86,
                    () -> ClaimMapModel.cancelTarget(center, radius, pageX, pageZ)));
        } else if (modes.startPick) {
            panel.addWidget(headerButton("Cancel", 60, width - 86, ClaimMapModel::cancelStartPick));
        }

        for (int j = viewport.startZ; j < viewport.startZ + viewport.visibleSize; j++) {
            for (int i = viewport.startX; i < viewport.startX + viewport.visibleSize; i++) {
                ClaimMapModel.Cell cellData = ClaimMapModel.cell(center, radius, i, j, modes);

                MapCellElement cell = new MapCellElement(cellData.textureName, cellData.renderInfo, cellData.adjacency, CELL_SIZE);
                cell.setSelfPosition(mapLeft + (i - viewport.startX) * CELL_SIZE, mapTop + (j - viewport.startZ) * CELL_SIZE);
                cell.onClick = button -> ClaimMapModel.click(center, radius, cellData, modes, button);
                cell.setHoverTooltips(ClaimMapModel.tooltip(center, cellData, modes));
                panel.addWidget(cell);
            }
        }

        activeScreen = ClientScreens.open(panel);
    }

    private static String legend(ClaimMapModel.Modes modes) {
        return modes.startPick
                ? "Click a highlighted chunk to launch the siege from. This consumes one siege camp block."
                : modes.target
                ? "Click an enemy claim to target it, then pick where to attack from."
                : "Left click claim or unclaim. Right click toggles force-loading. Use arrows to page the map.";
    }

    private static ButtonWidget headerButton(String label, int width, int x, Runnable action) {
        ButtonWidget button = WarForgeUiTheme.actionButton(label, width, click -> action.run());
        button.setSelfPosition(x, 9);
        button.setSize(width, 16);
        return button;
    }

    private static void addPanButtons(WidgetGroup panel, DimChunkPos center, int radius, ChunkMapViewport viewport,
                                      int sectionX, int sectionY, int sectionWidth, int sectionHeight) {
        if (viewport.canPanNorth()) {
            panel.addWidget(panButton("^", sectionX + sectionWidth / 2 - NAV_BUTTON_SIZE / 2, sectionY + 1,
                    center, radius, viewport.startX, viewport.panNorth()));
        }
        if (viewport.canPanSouth()) {
            panel.addWidget(panButton("v", sectionX + sectionWidth / 2 - NAV_BUTTON_SIZE / 2, sectionY + sectionHeight - NAV_BUTTON_SIZE - 1,
                    center, radius, viewport.startX, viewport.panSouth()));
        }
        if (viewport.canPanWest()) {
            panel.addWidget(panButton("<", sectionX + 1, sectionY + sectionHeight / 2 - NAV_BUTTON_SIZE / 2,
                    center, radius, viewport.panWest(), viewport.startZ));
        }
        if (viewport.canPanEast()) {
            panel.addWidget(panButton(">", sectionX + sectionWidth - NAV_BUTTON_SIZE - 1, sectionY + sectionHeight / 2 - NAV_BUTTON_SIZE / 2,
                    center, radius, viewport.panEast(), viewport.startZ));
        }
    }

    private static ButtonWidget panButton(String text, int x, int y, DimChunkPos center, int radius, int pageX, int pageZ) {
        ButtonWidget button = WarForgeUiTheme.actionButton(text, NAV_BUTTON_SIZE,
                click -> ClaimManagerGuiFactory.INSTANCE.openClient(center, radius, pageX, pageZ));
        button.setSelfPosition(x, y);
        button.setSize(NAV_BUTTON_SIZE, NAV_BUTTON_SIZE);
        return button;
    }

    /** The stripe colour: the faction owning the centre chunk, or grey for wilderness. */
    private static int accentColor(DimChunkPos center) {
        ClaimChunkInfo info = ClientClaimChunkCache.get(center);
        if (info != null && !info.factionId.equals(Faction.nullUuid)) {
            return info.colour;
        }
        return WarForgeUiTheme.STRIPE_DEFAULT;
    }
}
