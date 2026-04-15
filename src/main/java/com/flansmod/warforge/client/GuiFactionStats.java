package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
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
import net.minecraft.client.gui.Gui;
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
        ModularPanel panel = ModularPanel.defaultPanel("faction_stats")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(0, 0, WIDTH, 40, 0xFF171B1F, 0xFF0D1013)).size(WIDTH, 40));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, BODY_Y, WIDTH - CONTENT_LEFT * 2, 118, 0xEE20262B, 0xEE11161A)).size(WIDTH - CONTENT_LEFT * 2, 118).pos(CONTENT_LEFT, BODY_Y));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, ACTIONS_Y, WIDTH - CONTENT_LEFT * 2, 34, 0xEE20262B, 0xEE11161A)).size(WIDTH - CONTENT_LEFT * 2, 34).pos(CONTENT_LEFT, ACTIONS_Y));
        panel.child(new IDrawable.DrawableWidget(colorStripe(data.hasFaction ? (0xFF000000 | (data.factionColor & 0x00FFFFFF)) : 0xFF4A4A4A, 0, 0, 6, HEIGHT)).size(6, HEIGHT));
        panel.child(ButtonWidget.panelCloseButton().pos(WIDTH - 18, 8).size(10));

        if (!data.hasFaction) {
            panel.child(IKey.str("Faction Stats").asWidget()
                    .pos(CONTENT_LEFT, HEADER_Y)
                    .color(0xFFFFFF)
                    .style(TextFormatting.BOLD)
                    .shadow(true)
                    .scale(1.15f));
            panel.child(IKey.str("No faction information is available.").asWidget().pos(CONTENT_LEFT, HEADER_Y + 15).color(0xC7CCD1));
            return panel;
        }

        panel.child(IKey.str("Faction Stats").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y)
                .style(TextFormatting.BOLD)
                .color(0xFFFFFF)
                .shadow(true)
                .scale(1.15f));
        panel.child(IKey.str(data.factionName).asWidget()
                .pos(CONTENT_LEFT, HEADER_Y + 15)
                .color(data.factionColor)
                .style(TextFormatting.BOLD));

        var leaderRow = new Row()
                .padding(2)
                .child(new IDrawable.DrawableWidget(new PlayerFaceDrawable(data.leaderId)).size(20, 20).margin(2,0))
                .child(IKey.str("Leader: " + data.leaderName).asWidget().color(0xC7CCD1));

        panel.child(leaderRow.height(20).coverChildrenWidth().top(10).right(35));

        panel.child(IKey.str("Faction Overview").asWidget()
                        .color(0xFFFFFF)
                .pos(CONTENT_LEFT + 10, BODY_Y + 8)
                .style(TextFormatting.BOLD));
        panel.child(IKey.str("Standing, territory, membership, and citadel progression.").asWidget()
                .pos(CONTENT_LEFT + 10, BODY_Y + 20)
                .color(0xB8BDC3));

        int rowY = BODY_Y + 42;
        panel.child(statRow(CONTENT_LEFT + 10, rowY, "Notoriety", String.valueOf(data.notoriety), "#" + data.notorietyRank));
        panel.child(statRow(CONTENT_LEFT + 10, rowY + 18, "Wealth", String.valueOf(data.wealth), "#" + data.wealthRank));
        panel.child(statRow(CONTENT_LEFT + 10, rowY + 36, "Legacy", String.valueOf(data.legacy), "#" + data.legacyRank));
        panel.child(statRow(CONTENT_LEFT + 10, rowY + 54, "Total", String.valueOf(data.total), "#" + data.totalRank));
        panel.child(statRow(CONTENT_LEFT + 158, rowY, "Claims", String.valueOf(data.claimCount), data.claimLimit < 0 ? "INF" : String.valueOf(data.claimLimit)));
        panel.child(statRow(CONTENT_LEFT + 158, rowY + 18, "Members", String.valueOf(data.memberCount), ""));
        panel.child(statRow(CONTENT_LEFT + 158, rowY + 54, "Stash", String.valueOf(WarForgeMod.UPGRADE_HANDLER.getInsuranceSlotsForLevel(data.level)), "slots"));
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            panel.child(statRow(CONTENT_LEFT + 158, rowY + 36, "Citadel", "Lvl " + data.level, data.claimLimit < 0 ? "INF claims" : data.claimLimit + " claims"));
        }

        if (data.isOwnFaction) {
            panel.child(actionButton("Stash", CONTENT_LEFT + 10, ACTIONS_Y + 8, 66, () -> FactionInsuranceGuiFactory.INSTANCE.openClient(data.factionId)));
            panel.child(actionButton("Members", CONTENT_LEFT + 80, ACTIONS_Y + 8, 62, () -> FactionMemberManagerGuiFactory.INSTANCE.openClient(FactionMemberManagerGuiData.Page.MEMBERS)));
        }
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES && data.isOwnFaction) {
            panel.child(new ButtonWidget<>()
                    .width(70)
                    .height(18)
                    .overlay(IKey.str("Upgrade").color(data.canUpgrade ? 0xFFFFFF : 0x666666))
                    .background(data.canUpgrade ? GuiTextures.MC_BUTTON : GuiTextures.MC_BUTTON_DISABLED)
                    .hoverBackground(data.canUpgrade ? GuiTextures.MC_BUTTON_HOVERED : GuiTextures.MC_BUTTON_DISABLED)
                    .onMousePressed(mouseButton -> {
                        if (!data.canUpgrade) {
                            return false;
                        }
                        FactionUpgradeGuiFactory.INSTANCE.openClient(data.factionId);
                        return true;
                    })
                    .pos(CONTENT_LEFT + 146, ACTIONS_Y + 8));
        }
        panel.child(actionButton("Refresh", WIDTH - 88, ACTIONS_Y + 8, 66, () -> FactionStatsGuiFactory.INSTANCE.openClient(data.factionId)));

        return panel;
    }

    private static Widget statRow(int x, int y, String label, String value, String extra) {
        Row row = new Row();
        row.pos(x, y);
        row.width(136);
        row.height(16);
        row.child(IKey.str(label).asWidget().width(52).color(0xFFFFFF));
        row.child(IKey.str(value).asWidget().width(42).color(0xFFFFFF).style(TextFormatting.BOLD));
        row.child(IKey.str(extra).asWidget().width(42).color(0xBBBBBB));
        return row;
    }

    private static ButtonWidget<?> actionButton(String label, int x, int y, int width, Runnable action) {
        return new ButtonWidget<>()
                .width(width)
                .height(18)
                .overlay(IKey.str(label))
                .background(GuiTextures.MC_BUTTON)
                .hoverBackground(GuiTextures.MC_BUTTON_HOVERED)
                .onMousePressed(mouseButton -> {
                    action.run();
                    return true;
                })
                .pos(x, y);
    }

    private static IDrawable sectionBackdrop(int x, int y, int width, int height, int fillColor, int borderColor) {
        return new IDrawable() {
            @Override
            public void draw(GuiContext context, int drawX, int drawY, int drawWidth, int drawHeight, WidgetTheme theme) {
                Gui.drawRect(drawX, drawY, drawX + width, drawY + height, fillColor);
                Gui.drawRect(drawX, drawY, drawX + width, drawY + 1, borderColor);
                Gui.drawRect(drawX, drawY + height - 1, drawX + width, drawY + height, borderColor);
                Gui.drawRect(drawX, drawY, drawX + 1, drawY + height, borderColor);
                Gui.drawRect(drawX + width - 1, drawY, drawX + width, drawY + height, borderColor);
            }
        };
    }

    private static IDrawable colorStripe(int color, int x, int y, int width, int height) {
        return new IDrawable() {
            @Override
            public void draw(GuiContext context, int drawX, int drawY, int drawWidth, int drawHeight, WidgetTheme theme) {
                Gui.drawRect(drawX, drawY, drawX + width, drawY + height, color);
            }
        };
    }
}
