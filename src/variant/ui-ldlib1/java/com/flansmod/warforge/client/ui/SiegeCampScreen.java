package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.api.Time;
import com.flansmod.warforge.api.modularui.ChunkMapViewport;
import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.MapCellElement;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.ui.model.SiegeMapModel;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfoRender;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;
import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.HEADER_HEIGHT;

/** The siege target map of a siege camp (layout of the 1.20.1 GuiSiegeCamp). */
@OnlyIn(Dist.CLIENT)
public final class SiegeCampScreen {
    private static final int CELL_SIZE = 64;
    private static final int INFO_HEIGHT = 48;
    private static final int LEGEND_HEIGHT = 28;
    private static final int MAP_GUTTER = 18;
    private static final int SECTION_SPACING = 8;

    private SiegeCampScreen() {
    }

    public static void open(PacketSiegeCampInfo data) {
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

        panel.addWidget(WarForgeUiTheme.title("Siege Target Map"));
        panel.addWidget(Label.text(model.campLine(), WarForgeUiTheme.TEXT_SECONDARY)
                .clip(width - CONTENT_LEFT * 2 - 18).at(CONTENT_LEFT, WarForgeUiTheme.SUBTITLE_Y));

        int infoTextY = infoY + WarForgeUiTheme.SECTION_PADDING;
        Label momentum = Label.text("Current momentum: " + data.momentum, WarForgeUiTheme.TEXT_PRIMARY)
                .clip(textWidth).at(CONTENT_LEFT + 5, infoTextY);
        momentum.setHoverTooltips(momentumTooltip());
        panel.addWidget(momentum);
        panel.addWidget(Label.text("Select a target chunk around the siege camp. Center tile is your current camp position.",
                WarForgeUiTheme.TEXT_MUTED).clip(textWidth).at(CONTENT_LEFT + 5, infoTextY + 9 + 4));
        panel.addWidget(Label.text("Click an attackable chunk to begin the siege. Tooltips show faction ownership, claim type, and vein data.",
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
                cell.setHoverTooltips(styledTooltip(model.tooltip(chunkInfo), chunkInfo));
                panel.addWidget(cell);
            }
        }

        ClientScreens.open(panel);
    }

    private static List<Component> momentumTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6Momentum System§r"));
        lines.add(Component.literal("§7Momentum affects siege duration.§r"));
        lines.add(Component.literal("§7Gained via §aWON§7 sieges, lost on §cLOST§7 sieges.§r"));
        lines.add(Component.literal("§7Expires after §b" + WarForgeConfig.SIEGE_MOMENTUM_DURATION + " minutes§7.§r"));
        for (byte level : WarForgeConfig.SIEGE_MOMENTUM_TIME.keySet()) {
            long time = WarForgeConfig.SIEGE_MOMENTUM_TIME.get(level) * 1000L;
            lines.add(Component.literal("§eLevel " + level + "§r: §a"
                    + new Time(time).getFormattedTime(Time.TimeFormat.MINUTES_SECONDS, Time.Verbality.SHORT)));
        }
        return lines;
    }

    /** Colours the model's tooltip like the 1.20.1 menu: owner in faction colour, attack lines in red. */
    private static List<Component> styledTooltip(List<Component> lines, SiegeCampAttackInfoRender chunkInfo) {
        List<Component> styled = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            String text = line.getString();
            if (i == 0) {
                styled.add(chunkInfo.mFactionName.isEmpty()
                        ? line.copy().withStyle(ChatFormatting.GREEN)
                        : line.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(chunkInfo.mFactionColour & 0xFFFFFF))));
            } else if (chunkInfo.canAttack && i == lines.size() - 1) {
                styled.add(line.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
            } else if (chunkInfo.canAttack && i == lines.size() - 2 && text.startsWith("Attack time")) {
                styled.add(line.copy().withStyle(ChatFormatting.RED));
            } else {
                styled.add(line);
            }
        }
        return styled;
    }
}
