package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.ScrollArea;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.ui.model.UiText;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.OperationsGuiData;
import com.flansmod.warforge.common.network.PacketOperationsData;
import com.flansmod.warforge.common.network.PacketRequestFobWarp;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

/** The war room: sieges and FOBs (layout of the 1.20.1 GuiOperations). */
@OnlyIn(Dist.CLIENT)
public final class OperationsScreen {
    private static final int WIDTH = 372;
    private static final int HEIGHT = 268;
    private static final int SIEGE_Y = 48;
    private static final int FOB_Y = 150;
    private static final int SECTION_WIDTH = WIDTH - CONTENT_LEFT * 2;
    private static final int SIEGE_SECTION_HEIGHT = FOB_Y - SIEGE_Y - 6;
    private static final int FOB_SECTION_HEIGHT = HEIGHT - FOB_Y - 12;
    private static final int ROW_WIDTH = WIDTH - 44;
    private static final int ROW_HEIGHT = 24;

    private OperationsScreen() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        PacketOperationsData data = PacketOperationsData.latest;
        boolean hasFaction = data != null && data.hasFaction;

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, WIDTH, HEIGHT);
        panel.addWidget(WarForgeUiTheme.header(WIDTH));
        panel.addWidget(WarForgeUiTheme.stripe(hasFaction ? data.factionColor : WarForgeUiTheme.STRIPE_DEFAULT, HEIGHT));
        panel.addWidget(WarForgeUiTheme.closeButton(WIDTH, click -> ClientScreens.close()));
        panel.addWidget(WarForgeUiTheme.title("War Room"));

        if (!hasFaction) {
            panel.addWidget(WarForgeUiTheme.subtitle("You are not currently in a faction", WarForgeUiTheme.TEXT_SECONDARY, false));
            ClientScreens.open(panel);
            return;
        }

        panel.addWidget(WarForgeUiTheme.subtitle(data.factionName, data.factionColor, true));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, SIEGE_Y, SECTION_WIDTH, SIEGE_SECTION_HEIGHT));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, FOB_Y, SECTION_WIDTH, FOB_SECTION_HEIGHT));

        int textX = CONTENT_LEFT + 5;
        panel.addWidget(Label.bold("Active Sieges", WarForgeUiTheme.TEXT_MUTED).at(textX, SIEGE_Y + 5));
        ScrollArea siegeList = new ScrollArea(textX, SIEGE_Y + 5 + 9 + 4, SECTION_WIDTH - 10, SIEGE_SECTION_HEIGHT - 28);
        if (data.sieges.isEmpty()) {
            siegeList.addRow(Label.text("Your faction is not involved in any siege.", WarForgeUiTheme.TEXT_MUTED), 6, 6, 0);
        } else {
            for (OperationsGuiData.SiegeEntry siege : data.sieges) {
                siegeList.addRow(siegeRow(siege), 0, 0, 2);
            }
        }
        panel.addWidget(siegeList.finish());

        int y = FOB_Y + 5;
        panel.addWidget(Label.bold("Forward Operating Bases", WarForgeUiTheme.TEXT_MUTED).at(textX, y));
        y += 9 + 4;
        Label description = Label.text("Warp is available only while your faction is under siege. Tickets regenerate each siege cycle.",
                WarForgeUiTheme.TEXT_MUTED).wrap(SECTION_WIDTH - 10).at(textX, y);
        panel.addWidget(description);
        y += description.getSizeHeight() + 6;

        int listBottom = FOB_Y + FOB_SECTION_HEIGHT - 5;
        ScrollArea fobList = new ScrollArea(textX, y, SECTION_WIDTH - 10, Math.max(24, listBottom - y));
        if (data.fobs.isEmpty()) {
            fobList.addRow(Label.text("No FOBs established yet.", WarForgeUiTheme.TEXT_MUTED), 6, 6, 0);
        } else {
            for (OperationsGuiData.FobEntry fob : data.fobs) {
                fobList.addRow(fobRow(fob, data.warpTicketCost), 0, 0, 2);
            }
        }
        panel.addWidget(fobList.finish());

        ClientScreens.open(panel);
    }

    private static WidgetGroup rowFrame() {
        WidgetGroup row = new WidgetGroup(0, 0, ROW_WIDTH, ROW_HEIGHT);
        row.setBackground(WarForgeUiTheme.insetBackdrop(WarForgeUiTheme.ROW_FILL));
        return row;
    }

    private static WidgetGroup siegeRow(OperationsGuiData.SiegeEntry siege) {
        WidgetGroup row = rowFrame();
        int cellHeight = ROW_HEIGHT - 6;
        int roleColor = siege.attacking ? UiText.STATUS_ORANGE : WarForgeUiTheme.TEXT_SUCCESS;
        row.addWidget(Label.text(siege.attacking ? "Attacking" : "Defending", roleColor).box(66, cellHeight).at(3, 3));
        String versus = siege.attackerName + " vs " + siege.defenderName;
        row.addWidget(Label.text(versus, WarForgeUiTheme.TEXT_PRIMARY).box(158, cellHeight).tooltip(versus).at(3 + 66 + 5, 3));
        row.addWidget(Label.text("[" + siege.chunkX + ", " + siege.chunkZ + "]", WarForgeUiTheme.TEXT_SECONDARY)
                .box(70, cellHeight).at(3 + 66 + 5 + 158 + 5, 3));
        return row;
    }

    private static WidgetGroup fobRow(OperationsGuiData.FobEntry fob, int warpTicketCost) {
        WidgetGroup row = rowFrame();
        int cellHeight = ROW_HEIGHT - 6;
        int color = UiText.fobStatusColor(fob);
        row.addWidget(Label.text(fob.name.isEmpty() ? "FOB" : fob.name, color).box(150, cellHeight)
                .tooltip(fob.occupied ? "Occupied by enemies" : fob.pos.toFancyString()).at(3 + 5, 3));
        row.addWidget(Label.text(fob.tickets + " / " + fob.maxTickets, color).box(58, cellHeight).at(3 + 5 + 150 + 5, 3));

        DimBlockPos pos = fob.pos;
        ButtonWidget warp = WarForgeUiTheme.actionButton("Warp", 52, fob.canWarp, click -> {
            PacketRequestFobWarp packet = new PacketRequestFobWarp();
            packet.mPos = pos;
            WarForgeMod.NETWORK.sendToServer(packet);
        });
        warp.setHoverTooltips("Warp cost: " + warpTicketCost + (warpTicketCost == 1 ? " ticket" : " tickets"));
        row.addWidget(WarForgeUiTheme.at(warp, 3 + 5 + 150 + 5 + 58, 3));
        return row;
    }
}
