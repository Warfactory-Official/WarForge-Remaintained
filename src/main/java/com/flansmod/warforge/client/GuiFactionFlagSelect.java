package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.flansmod.warforge.client.util.FlagDrawable;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionFlagSelectGuiData;
import com.flansmod.warforge.common.network.PacketChooseFactionFlag;
import net.minecraft.util.text.TextFormatting;

public final class GuiFactionFlagSelect {
    private static final int WIDTH = 332;
    private static final int HEIGHT = 248;
    private static final int CONTENT_LEFT = 12;
    private static final int BODY_Y = 54;

    private GuiFactionFlagSelect() {
    }

    public static ModularPanel buildPanel(FactionFlagSelectGuiData data) {
        int sectionWidth = WIDTH - CONTENT_LEFT * 2;
        int bodyHeight = HEIGHT - BODY_Y - 12;

        ModularPanel panel = ModularPanel.defaultPanel("faction_flag_select")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        Flow bodySection = ModularGuiStyle.section(sectionWidth, bodyHeight).name("faction_flag_body_section").pos(CONTENT_LEFT, BODY_Y);

        panel.child(new IDrawable.DrawableWidget(ModularGuiStyle.headerBackdrop()).size(WIDTH, 40));
        panel.child(bodySection);
        panel.child(new IDrawable.DrawableWidget(ModularGuiStyle.colorStripe(data.factionColor)).size(6, HEIGHT));
        panel.child(ModularGuiStyle.panelCloseButton(WIDTH));

        panel.child(IKey.str("Faction Flag").asWidget().pos(CONTENT_LEFT, 12).style(TextFormatting.BOLD).color(ModularGuiStyle.TEXT_PRIMARY).shadow(true).scale(1.15f));
        panel.child(IKey.str(data.factionName).asWidget().pos(CONTENT_LEFT, 27).color(data.factionColor).style(TextFormatting.BOLD));
        bodySection.child(IKey.str(data.currentFlagId.isEmpty() ? "Choose once. This cannot be changed later." : "Flag locked: " + displayName(data.currentFlagId)).asWidget()
                .margin(0, 0, 0, 6)
                .color(ModularGuiStyle.TEXT_SECONDARY));

        ListWidget list = new ListWidget<>()
                .name("faction_flag_list")
                .scrollDirection(GuiAxis.Y)
                .background(ModularGuiStyle.insetBackdrop())
                .width(sectionWidth - 10)
                .height(bodyHeight - 30);

        int index = 0;
        for (String flagId : data.availableFlags) {
            list.addChild(createRow(data, flagId), index++);
        }
        bodySection.child(list);
        return panel;
    }

    private static Row createRow(FactionFlagSelectGuiData data, String flagId) {
        Row row = new Row();
        row.name(ModularGuiStyle.debugName("flag_row", flagId));
        row.width(WIDTH - 40);
        row.height(28);
        row.padding(3, 2);
        row.margin(0, 0, 0, 2);
        row.background(ModularGuiStyle.insetBackdrop(0xFF232A30));
        row.child(new IDrawable.DrawableWidget(new FlagDrawable(flagId)).size(42, 24));
        row.child(new ScrollingTextWidget(IKey.str(displayName(flagId)).color(ModularGuiStyle.TEXT_SECONDARY)).width(160).tooltip(t -> t.addLine(flagId)));
        boolean chosen = flagId.equals(data.currentFlagId);
        boolean clickable = data.canChoose && data.currentFlagId.isEmpty();
        if (chosen) {
            row.child(ModularGuiStyle.tabButton("Selected", 74, true, () -> { }));
        } else {
            row.child(ModularGuiStyle.actionButton("Choose", 74, clickable, () -> {
                PacketChooseFactionFlag packet = new PacketChooseFactionFlag();
                packet.factionId = data.factionId;
                packet.flagId = flagId;
                WarForgeMod.NETWORK.sendToServer(packet);
            }));
        }
        return row;
    }

    private static String displayName(String flagId) {
        String raw = flagId.contains(":") ? flagId.substring(flagId.indexOf(':') + 1) : flagId;
        return raw.replace('_', ' ');
    }
}
