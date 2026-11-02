package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.client.ui.model.DataRevision;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;

import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.util.PlayerFaceElement;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.network.PacketRequestFactionInfo;
import com.flansmod.warforge.common.network.PlayerDisplayInfo;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

@OnlyIn(Dist.CLIENT)
public final class FactionStatsScreen {
    private static final DataRevision REVISION = new DataRevision();
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
        if (info != null && REVISION.unchanged(Minecraft.getInstance().screen, info)) {
            return;
        }

        boolean hasFaction = info != null && !info.factionId.equals(FactionIds.NONE);

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
        panel.addWidget(WarForgeUiTheme.title(tr("warforge.ui.faction_stats")));

        if (!hasFaction) {
            panel.addWidget(WarForgeUiTheme.subtitle(tr("warforge.ui.no_faction_information_is_available"), WarForgeUiTheme.TEXT_SECONDARY, false));
            REVISION.shown(ClientScreens.open(panel));
            return;
        }

        panel.addWidget(WarForgeUiTheme.subtitle(info.factionName, info.colour, true));

        PlayerDisplayInfo leader = info.getPlayerInfo(info.leaderId);
        Label leaderText = Label.text(tr("warforge.ui.leader_s", leader == null ? tr("warforge.ui.unknown") : leader.username), WarForgeUiTheme.TEXT_SECONDARY);
        int leaderRowWidth = 2 + 2 + 20 + 2 + leaderText.getSizeWidth() + 2;
        int leaderX = WIDTH - 35 - leaderRowWidth;
        panel.addWidget(WarForgeUiTheme.at(new PlayerFaceElement(info.leaderId, 20), leaderX + 4, 10));
        panel.addWidget(leaderText.at(leaderX + 4 + 20 + 2, 16));

        int textX = CONTENT_LEFT + 5;
        panel.addWidget(Label.bold(tr("warforge.ui.faction_overview"), WarForgeUiTheme.TEXT_PRIMARY).at(textX, BODY_Y + 5));
        panel.addWidget(Label.text(tr("warforge.ui.standing_territory_membership_and_citadel_progre"), WarForgeUiTheme.TEXT_MUTED)
                .clip(SECTION_WIDTH - 10).at(textX, BODY_Y + 5 + 9 + 4));

        int total = info.notoriety + info.wealth + info.legacy;
        int claimLimit = com.flansmod.warforge.client.ClientUpgradeTable.getClaimLimitForLevel(info.lvl);
        int stashSlots = com.flansmod.warforge.client.ClientUpgradeTable.getInsuranceSlotsForLevel(info.lvl);

        int gridX = textX;
        int gridY = BODY_Y + 5 + 9 + 4 + 9 + 6;
        statRow(panel, gridX, gridY, tr("warforge.ui.notoriety"), String.valueOf(info.notoriety), "#" + info.notorietyRank);
        statRow(panel, gridX, gridY + 18, tr("warforge.ui.wealth"), String.valueOf(info.wealth), "#" + info.wealthRank);
        statRow(panel, gridX, gridY + 36, tr("warforge.ui.legacy"), String.valueOf(info.legacy), "#" + info.legacyRank);
        statRow(panel, gridX, gridY + 54, tr("warforge.ui.total"), String.valueOf(total), "#" + info.totalRank);
        statRow(panel, gridX + 148, gridY, tr("warforge.ui.claims"), String.valueOf(info.numClaims), claimLimit < 0 ? tr("warforge.ui.inf") : String.valueOf(claimLimit));
        statRow(panel, gridX + 148, gridY + 18, tr("warforge.ui.members"), String.valueOf(info.members.size()), "");
        statRow(panel, gridX + 148, gridY + 54, tr("warforge.ui.stash"), String.valueOf(stashSlots), tr("warforge.ui.slots"));
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            statRow(panel, gridX + 148, gridY + 36, tr("warforge.ui.citadel"), tr("warforge.ui.lvl_d", info.lvl), claimLimit < 0 ? tr("warforge.ui.inf_claims") : tr("warforge.ui.d_claims", claimLimit));
        }

        boolean isOfficer = self != null && self.role.ordinal() >= Role.OFFICER.ordinal();
        UUID displayedFaction = info.factionId;

        int rowY = ACTIONS_Y + 5 + 2;
        int x = textX;
        if (isOwnFaction) {
            x = place(panel, WarForgeUiTheme.actionButton(tr("warforge.ui.stash"), 66,
                    click -> UiNavigation.requestInsurance(displayedFaction)), x, rowY, 0);
            x = place(panel, WarForgeUiTheme.actionButton(tr("warforge.ui.members"), 62,
                    click -> UiNavigation.requestMembers(FactionMemberManagerGuiData.Page.MEMBERS)), x, rowY, 4);
            if (WarForgeConfig.ENABLE_CITADEL_UPGRADES && isOfficer) {
                x = place(panel, WarForgeUiTheme.actionButton(tr("warforge.ui.upgrade"), 70,
                        click -> UiNavigation.requestUpgrade(displayedFaction)), x, rowY, 4);
            }
        }
        place(panel, WarForgeUiTheme.actionButton(tr("warforge.ui.refresh"), 66, click -> {
            PacketRequestFactionInfo packet = new PacketRequestFactionInfo();
            packet.factionIdRequest = displayedFaction;
            WarForgeMod.NETWORK.sendToServer(packet);
        }), x, rowY, isOwnFaction ? 4 : 0);

        if (twoRows) {
            int secondY = rowY + ROW_STEP;
            int x2 = place(panel, WarForgeUiTheme.actionButton(tr("warforge.ui.operations"), 84, click -> UiNavigation.requestOperations()), textX, secondY, 0);
            place(panel, WarForgeUiTheme.actionButton(tr("warforge.ui.flag"), 66,
                    click -> UiNavigation.openFlagSelect(displayedFaction, info.factionName, info.colour, info.flagId)), x2, secondY, 4);
        }

        REVISION.shown(ClientScreens.open(panel));
    }

    private static int place(WidgetGroup panel, ButtonWidget button, int x, int y, int margin) {
        button.setSelfPosition(x + margin, y);
        panel.addWidget(button);
        return x + margin + button.getSizeWidth() + margin;
    }

    private static void statRow(WidgetGroup panel, int x, int y, String label, String value, String extra) {
        panel.addWidget(Label.text(label, WarForgeUiTheme.TEXT_PRIMARY).box(52, 16).at(x, y));
        panel.addWidget(Label.bold(value, WarForgeUiTheme.TEXT_PRIMARY).box(42, 16).at(x + 52, y));
        panel.addWidget(Label.text(extra, WarForgeUiTheme.TEXT_DIM).box(42, 16).at(x + 94, y));
    }
}
