package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.flansmod.warforge.client.util.PlayerFaceDrawable;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.factories.FactionInsuranceGuiFactory;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiFactory;
import com.flansmod.warforge.common.factories.FactionStatsGuiData;
import com.flansmod.warforge.common.factories.FactionStatsGuiFactory;
import com.flansmod.warforge.common.factories.FactionUpgradeGuiFactory;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.util.text.TextFormatting;

public final class GuiFactionStats {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 228;
    private static final int CONTENT_LEFT = 12;
    private static final int HEADER_Y = 12;
    private static final int BODY_Y = 54;
    private static final int ACTIONS_Y = 182;

    private GuiFactionStats() {
    }

    public static ModularPanel buildPanel(FactionStatsGuiData data) {
        int sectionWidth = WIDTH - CONTENT_LEFT * 2;

        ModularPanel panel = ModularPanel.defaultPanel("faction_stats")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        Flow bodySection = ModularGuiStyle.section(sectionWidth, 118).name("faction_stats_body_section").pos(CONTENT_LEFT, BODY_Y);
        Flow actionSection = ModularGuiStyle.section(sectionWidth, 34).name("faction_stats_action_section").pos(CONTENT_LEFT, ACTIONS_Y);

        panel.child(new com.cleanroommc.modularui.api.drawable.IDrawable.DrawableWidget(ModularGuiStyle.headerBackdrop()).size(WIDTH, 40));
        panel.child(bodySection);
        panel.child(actionSection);
        panel.child(new com.cleanroommc.modularui.api.drawable.IDrawable.DrawableWidget(ModularGuiStyle.colorStripe(data.hasFaction ? data.factionColor : 0x4A4A4A)).size(6, HEIGHT));
        panel.child(ModularGuiStyle.panelCloseButton(WIDTH));

        if (!data.hasFaction) {
            panel.child(IKey.str("Faction Stats").asWidget()
                    .pos(CONTENT_LEFT, HEADER_Y)
                    .color(ModularGuiStyle.TEXT_PRIMARY)
                    .style(TextFormatting.BOLD)
                    .shadow(true)
                    .scale(1.15f));
            panel.child(IKey.str("No faction information is available.").asWidget().pos(CONTENT_LEFT, HEADER_Y + 15).color(ModularGuiStyle.TEXT_SECONDARY));
            return panel;
        }

        panel.child(IKey.str("Faction Stats").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y)
                .style(TextFormatting.BOLD)
                .color(ModularGuiStyle.TEXT_PRIMARY)
                .shadow(true)
                .scale(1.15f));
        panel.child(IKey.str(data.factionName).asWidget()
                .pos(CONTENT_LEFT, HEADER_Y + 15)
                .color(data.factionColor)
                .style(TextFormatting.BOLD));

        var leaderRow = new Flow(GuiAxis.X)
                .name("faction_stats_leader_row")
                .padding(2)
                .child(new IDrawable.DrawableWidget(new PlayerFaceDrawable(data.leaderId)).size(20, 20).margin(2,0))
                .child(IKey.str("Leader: " + data.leaderName).asWidget().color(ModularGuiStyle.TEXT_SECONDARY));

        panel.child(leaderRow.height(20).coverChildrenWidth().top(10).right(35));

        bodySection.child(IKey.str("Faction Overview").asWidget()
                .color(ModularGuiStyle.TEXT_PRIMARY)
                .margin(0, 0, 0, 4)
                .style(TextFormatting.BOLD));
        bodySection.child(IKey.str("Standing, territory, membership, and citadel progression.").asWidget()
                .margin(0, 0, 0, 6)
                .color(ModularGuiStyle.TEXT_MUTED));

        ParentWidget<?> statGrid = new ParentWidget<>();
        statGrid.name("faction_stats_grid");
        statGrid.size(sectionWidth - 10, 70);
        statGrid.child(statRow(0, 0, "Notoriety", String.valueOf(data.notoriety), "#" + data.notorietyRank));
        statGrid.child(statRow(0, 18, "Wealth", String.valueOf(data.wealth), "#" + data.wealthRank));
        statGrid.child(statRow(0, 36, "Legacy", String.valueOf(data.legacy), "#" + data.legacyRank));
        statGrid.child(statRow(0, 54, "Total", String.valueOf(data.total), "#" + data.totalRank));
        statGrid.child(statRow(148, 0, "Claims", String.valueOf(data.claimCount), data.claimLimit < 0 ? "INF" : String.valueOf(data.claimLimit)));
        statGrid.child(statRow(148, 18, "Members", String.valueOf(data.memberCount), ""));
        statGrid.child(statRow(148, 54, "Stash", String.valueOf(WarForgeMod.UPGRADE_HANDLER.getInsuranceSlotsForLevel(data.level)), "slots"));
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            statGrid.child(statRow(148, 36, "Citadel", "Lvl " + data.level, data.claimLimit < 0 ? "INF claims" : data.claimLimit + " claims"));
        }
        bodySection.child(statGrid);

        var actionRow = new Flow(GuiAxis.X).name("faction_stats_action_row");
        actionRow.height(18+8).paddingTop(2);

        actionRow.width(sectionWidth - 10);
        ButtonWidget<?> upgradeButton = null;
        if (data.isOwnFaction) {
            actionRow.child(ModularGuiStyle.actionButton("Stash", 66, () -> FactionInsuranceGuiFactory.INSTANCE.openClient(data.factionId)));
            actionRow.child(ModularGuiStyle.actionButton("Members", 62, () -> FactionMemberManagerGuiFactory.INSTANCE.openClient(FactionMemberManagerGuiData.Page.MEMBERS)).margin(4, 0));
        }
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES && data.isOwnFaction) {
            upgradeButton = ModularGuiStyle.actionButton("Upgrade", 70, data.canUpgrade, () -> FactionUpgradeGuiFactory.INSTANCE.openClient(data.factionId));
            actionRow.child(upgradeButton.margin(4, 0));
        }
        actionRow.child(ModularGuiStyle.actionButton("Refresh", 66, () -> FactionStatsGuiFactory.INSTANCE.openClient(data.factionId)).margin(4, 0));
        actionSection.child(actionRow);

        if (upgradeButton != null && !data.canUpgrade) {
            upgradeButton.tooltip(tooltip -> tooltip.addLine("Citadel is already at the maximum level."));
        }

        return panel;
    }

    private static Widget statRow(int x, int y, String label, String value, String extra) {
        var row = new Flow(GuiAxis.X).name(ModularGuiStyle.debugName("stat_row", label));
        row.pos(x, y);
        row.width(136);
        row.height(16);
        row.child(IKey.str(label).asWidget().width(52).color(ModularGuiStyle.TEXT_PRIMARY));
        row.child(IKey.str(value).asWidget().width(42).color(ModularGuiStyle.TEXT_PRIMARY).style(TextFormatting.BOLD));
        row.child(IKey.str(extra).asWidget().width(42).color(0xBBBBBB));
        return row;
    }
}
