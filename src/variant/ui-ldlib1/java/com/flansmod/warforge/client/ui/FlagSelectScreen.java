package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.ScrollArea;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.ClientFlagRegistry;
import com.flansmod.warforge.client.ui.model.UiText;
import com.flansmod.warforge.client.util.FlagElement;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketChooseFactionFlag;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

/** Faction flag selection (layout of the 1.20.1 GuiFactionFlagSelect). */
@OnlyIn(Dist.CLIENT)
public final class FlagSelectScreen {
    private static final int WIDTH = 332;
    private static final int HEIGHT = 248;
    private static final int BODY_Y = 54;
    private static final int SECTION_WIDTH = WIDTH - CONTENT_LEFT * 2;
    private static final int BODY_HEIGHT = HEIGHT - BODY_Y - 12;
    private static final int ROW_WIDTH = WIDTH - 40;
    private static final int ROW_HEIGHT = 28;

    private FlagSelectScreen() {
    }

    public static void open(UUID factionId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        Faction faction = WarForgeMod.FACTIONS.getFaction(factionId);
        String factionName = faction != null ? faction.name : "";
        int factionColour = faction != null ? faction.colour : 0xFFFFFF;
        String currentFlagId = faction != null ? faction.flagId : "";
        boolean canChoose = currentFlagId.isEmpty();

        List<String> availableFlags = ClientFlagRegistry.getAvailableFlags();

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, WIDTH, HEIGHT);
        panel.addWidget(WarForgeUiTheme.header(WIDTH));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, BODY_Y, SECTION_WIDTH, BODY_HEIGHT));
        panel.addWidget(WarForgeUiTheme.stripe(factionColour, HEIGHT));
        panel.addWidget(WarForgeUiTheme.closeButton(WIDTH, click -> ClientScreens.close()));
        panel.addWidget(WarForgeUiTheme.title("Faction Flag"));
        if (!factionName.isEmpty()) {
            panel.addWidget(WarForgeUiTheme.subtitle(factionName, factionColour, true));
        }

        int textX = CONTENT_LEFT + 5;
        String statusText = canChoose
                ? "Choose once. This cannot be changed later."
                : "Flag locked: " + UiText.flagDisplayName(currentFlagId);
        panel.addWidget(Label.text(statusText, WarForgeUiTheme.TEXT_SECONDARY).clip(SECTION_WIDTH - 10).at(textX, BODY_Y + 5));

        ScrollArea list = new ScrollArea(textX, BODY_Y + 5 + 9 + 6, SECTION_WIDTH - 10, BODY_HEIGHT - 30);
        for (String flagId : availableFlags) {
            list.addRow(row(flagId, factionId, currentFlagId, canChoose), 0, 0, 2);
        }
        panel.addWidget(list.finish());

        ClientScreens.open(panel);
    }

    private static WidgetGroup row(String flagId, UUID factionId, String currentFlagId, boolean canChoose) {
        WidgetGroup row = new WidgetGroup(0, 0, ROW_WIDTH, ROW_HEIGHT);
        row.setBackground(WarForgeUiTheme.insetBackdrop(WarForgeUiTheme.ROW_FILL));
        row.addWidget(WarForgeUiTheme.at(new FlagElement(flagId, 42, 24), 3, 2));
        row.addWidget(Label.text(UiText.flagDisplayName(flagId), WarForgeUiTheme.TEXT_PRIMARY)
                .box(160, ROW_HEIGHT).tooltip(flagId).at(3 + 42, 0));

        ButtonWidget button;
        if (flagId.equals(currentFlagId)) {
            button = WarForgeUiTheme.tabButton("Selected", 74, true, null);
        } else {
            button = WarForgeUiTheme.actionButton("Choose", 74, canChoose, click -> {
                PacketChooseFactionFlag packet = new PacketChooseFactionFlag();
                packet.factionId = factionId;
                packet.flagId = flagId;
                WarForgeMod.NETWORK.sendToServer(packet);
            });
        }
        row.addWidget(WarForgeUiTheme.at(button, 3 + 42 + 160, (ROW_HEIGHT - WarForgeUiTheme.BUTTON_HEIGHT) / 2));
        return row;
    }
}
