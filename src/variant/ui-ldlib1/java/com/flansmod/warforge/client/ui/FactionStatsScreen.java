package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.util.PlayerFaceElement;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionFlagSelectGuiFactory;
import com.flansmod.warforge.common.factories.FactionInsuranceGuiFactory;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiFactory;
import com.flansmod.warforge.common.factories.FactionUpgradeGuiFactory;
import com.flansmod.warforge.common.factories.OperationsGuiFactory;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.network.PacketRequestFactionInfo;
import com.flansmod.warforge.common.network.PlayerDisplayInfo;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

/**
 * Faction stats (layout of the 1.20.1 GuiFactionStats). The action section has a second row for the Operations and
 * Flag buttons, which the 1.20.1 menu does not have, so the panel is 22px taller.
 */
@OnlyIn(Dist.CLIENT)
public final class FactionStatsScreen {
    private static final int WIDTH = 320;
    private static final int BODY_Y = 54;
    private static final int ACTIONS_Y = 182;
    private static final int ROW_STEP = 22;
    private static final int SECTION_WIDTH = WIDTH - CONTENT_LEFT * 2;

    private FactionStatsScreen() {
    }

    public static void open(UUID factionId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        FactionDisplayInfo info = PacketFactionInfo.latestInfo;
        boolean hasFaction = info != null && !info.factionId.equals(Faction.nullUuid);

        PlayerDisplayInfo self = hasFaction ? info.getPlayerInfo(mc.player.getUUID()) : null;
        boolean isOwnFaction = self != null;
        boolean twoRows = isOwnFaction;
        int actionHeight = twoRows ? 34 + ROW_STEP : 34;
        int height = ACTIONS_Y + actionHeight + 12;

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, WIDTH, height);
        panel.addWidget(WarForgeUiTheme.header(WIDTH));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, BODY_Y, SECTION_WIDTH, 118));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, ACTIONS_Y, SECTION_WIDTH, actionHeight));
        panel.addWidget(WarForgeUiTheme.stripe(hasFaction ? info.colour : WarForgeUiTheme.STRIPE_DEFAULT, height));
        panel.addWidget(WarForgeUiTheme.closeButton(WIDTH, click -> ClientScreens.close()));
        panel.addWidget(WarForgeUiTheme.title("Faction Stats"));

        if (!hasFaction) {
            panel.addWidget(WarForgeUiTheme.subtitle("No faction information is available.", WarForgeUiTheme.TEXT_SECONDARY, false));
            ClientScreens.open(panel);
            return;
        }

        panel.addWidget(WarForgeUiTheme.subtitle(info.factionName, info.colour, true));

        // Leader row in the header, right aligned 35px from the edge.
        PlayerDisplayInfo leader = info.getPlayerInfo(info.mLeaderID);
        Label leaderText = Label.text("Leader: " + (leader == null ? "Unknown" : leader.username), WarForgeUiTheme.TEXT_SECONDARY);
        int leaderRowWidth = 2 + 2 + 20 + 2 + leaderText.getSizeWidth() + 2;
        int leaderX = WIDTH - 35 - leaderRowWidth;
        panel.addWidget(WarForgeUiTheme.at(new PlayerFaceElement(info.mLeaderID, 20), leaderX + 4, 10));
        panel.addWidget(leaderText.at(leaderX + 4 + 20 + 2, 16));

        int textX = CONTENT_LEFT + 5;
        panel.addWidget(Label.bold("Faction Overview", WarForgeUiTheme.TEXT_PRIMARY).at(textX, BODY_Y + 5));
        panel.addWidget(Label.text("Standing, territory, membership, and citadel progression.", WarForgeUiTheme.TEXT_MUTED)
                .clip(SECTION_WIDTH - 10).at(textX, BODY_Y + 5 + 9 + 4));

        int total = info.notoriety + info.wealth + info.legacy;
        int claimLimit = WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(info.lvl);
        int stashSlots = WarForgeMod.UPGRADE_HANDLER.getInsuranceSlotsForLevel(info.lvl);

        int gridX = textX;
        int gridY = BODY_Y + 5 + 9 + 4 + 9 + 6;
        statRow(panel, gridX, gridY, "Notoriety", String.valueOf(info.notoriety), "#" + info.notorietyRank);
        statRow(panel, gridX, gridY + 18, "Wealth", String.valueOf(info.wealth), "#" + info.wealthRank);
        statRow(panel, gridX, gridY + 36, "Legacy", String.valueOf(info.legacy), "#" + info.legacyRank);
        statRow(panel, gridX, gridY + 54, "Total", String.valueOf(total), "#" + info.totalRank);
        statRow(panel, gridX + 148, gridY, "Claims", String.valueOf(info.mNumClaims), claimLimit < 0 ? "INF" : String.valueOf(claimLimit));
        statRow(panel, gridX + 148, gridY + 18, "Members", String.valueOf(info.members.size()), "");
        statRow(panel, gridX + 148, gridY + 54, "Stash", String.valueOf(stashSlots), "slots");
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            statRow(panel, gridX + 148, gridY + 36, "Citadel", "Lvl " + info.lvl, claimLimit < 0 ? "INF claims" : claimLimit + " claims");
        }

        boolean isOfficer = self != null && self.role.ordinal() >= Faction.Role.OFFICER.ordinal();
        UUID displayedFaction = info.factionId;

        int rowY = ACTIONS_Y + 5 + 2;
        int x = textX;
        if (isOwnFaction) {
            x = place(panel, WarForgeUiTheme.actionButton("Stash", 66,
                    click -> FactionInsuranceGuiFactory.INSTANCE.openClientSibling(displayedFaction)), x, rowY, 0);
            x = place(panel, WarForgeUiTheme.actionButton("Members", 62,
                    click -> FactionMemberManagerGuiFactory.INSTANCE.openClientSibling(FactionMemberManagerGuiData.Page.MEMBERS)), x, rowY, 4);
            if (WarForgeConfig.ENABLE_CITADEL_UPGRADES && isOfficer) {
                x = place(panel, WarForgeUiTheme.actionButton("Upgrade", 70,
                        click -> FactionUpgradeGuiFactory.INSTANCE.openClientSibling(displayedFaction)), x, rowY, 4);
            }
        }
        place(panel, WarForgeUiTheme.actionButton("Refresh", 66, click -> {
            PacketRequestFactionInfo packet = new PacketRequestFactionInfo();
            packet.mFactionIDRequest = displayedFaction;
            WarForgeMod.NETWORK.sendToServer(packet);
        }), x, rowY, isOwnFaction ? 4 : 0);

        if (twoRows) {
            int secondY = rowY + ROW_STEP;
            int x2 = place(panel, WarForgeUiTheme.actionButton("Operations", 84, click -> OperationsGuiFactory.INSTANCE.openClient()), textX, secondY, 0);
            place(panel, WarForgeUiTheme.actionButton("Flag", 66,
                    click -> FactionFlagSelectGuiFactory.INSTANCE.openClient(displayedFaction)), x2, secondY, 4);
        }

        ClientScreens.open(panel);
    }

    /** Places a button after x with the margin on both sides (a ModularUI row child); returns the next x. */
    private static int place(WidgetGroup panel, ButtonWidget button, int x, int y, int margin) {
        button.setSelfPosition(x + margin, y);
        panel.addWidget(button);
        return x + margin + button.getSizeWidth() + margin;
    }

    private static void statRow(WidgetGroup panel, int x, int y, String label, String value, String extra) {
        panel.addWidget(Label.text(label, WarForgeUiTheme.TEXT_PRIMARY).box(52, 16).at(x, y));
        panel.addWidget(Label.bold(value, WarForgeUiTheme.TEXT_PRIMARY).box(42, 16).at(x + 52, y));
        panel.addWidget(Label.text(extra, 0xBBBBBB).box(42, 16).at(x + 94, y));
    }
}
