package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.compat.ClientCompat;
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
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

/** Faction stats, laid out like GuiFactionStats of the 1.20.1 build. */
@OnlyIn(Dist.CLIENT)
public final class FactionStatsScreen {
    private static final int WIDTH = 320;
    private static final int BASE_HEIGHT = 228;
    private static final int ACTIONS_Y = 182;
    private static final int ACTION_ROW_HEIGHT = 22;

    private FactionStatsScreen() {
    }

    public static void open(UUID factionId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        FactionDisplayInfo info = PacketFactionInfo.latestInfo;
        boolean hasFaction = info != null && !info.factionId.equals(Faction.nullUuid);
        int sectionWidth = WIDTH - CONTENT_LEFT * 2;
        int rowWidth = sectionWidth - 10;

        // The action buttons (this build has more than the 1.20.1 one): wrap onto extra rows when needed.
        List<Action> buttons = new ArrayList<>();
        if (hasFaction) {
            buttons = actionButtons(mc, info);
        }
        int rows = countRows(buttons, rowWidth);
        int height = BASE_HEIGHT + (rows - 1) * ACTION_ROW_HEIGHT;

        UIElement root = screenRoot(0);
        UIElement panel = panel(WIDTH, height);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        UIElement body = section(panel, CONTENT_LEFT, BODY_Y, sectionWidth, 118);
        UIElement actionSection = section(panel, CONTENT_LEFT, ACTIONS_Y, sectionWidth, 34 + (rows - 1) * ACTION_ROW_HEIGHT);
        colorStripe(panel, hasFaction ? info.colour : NO_FACTION_STRIPE, height);
        closeButton(panel, WIDTH).setOnClick(event -> ClientCompat.setScreen(null));

        title(panel, "Faction Stats", TEXT_PRIMARY, 180);

        if (!hasFaction) {
            subtitle(panel, text("No faction information is available.", TEXT_SECONDARY), WIDTH - CONTENT_LEFT * 2 - 18);
            show(root, mc);
            return;
        }

        subtitle(panel, boldText(info.factionName, info.colour), 180);

        // Leader, top right of the header
        PlayerDisplayInfo leader = info.getPlayerInfo(info.mLeaderID);
        String leaderText = "Leader: " + (leader == null ? "Unknown" : leader.username);
        int leaderTextWidth = mc.font.width(leaderText);
        int leaderRowWidth = 2 + 2 + 20 + 2 + leaderTextWidth + 2;
        UIElement leaderRow = place(panel, row(leaderRowWidth, 20), WIDTH - 35 - leaderRowWidth, 10, leaderRowWidth, 20);
        leaderRow.layout(l -> l.paddingHorizontal(2));
        PlayerFaceElement leaderFace = new PlayerFaceElement(info.mLeaderID);
        leaderFace.layout(l -> l.width(20).height(20).marginHorizontal(2).flexShrink(0));
        leaderRow.addChild(leaderFace);
        leaderRow.addChild(width(text(leaderText, TEXT_SECONDARY), leaderTextWidth + 1));

        Label overview = width(boldText("Faction Overview", TEXT_PRIMARY), rowWidth);
        overview.layout(l -> l.marginBottom(4));
        body.addChild(overview);
        Label blurb = width(text("Standing, territory, membership, and citadel progression.", TEXT_MUTED), rowWidth);
        blurb.layout(l -> l.marginBottom(6));
        body.addChild(blurb);

        int total = info.notoriety + info.wealth + info.legacy;
        int claimLimit = WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(info.lvl);
        int stashSlots = WarForgeMod.UPGRADE_HANDLER.getInsuranceSlotsForLevel(info.lvl);

        UIElement grid = box(rowWidth, 70);
        statRow(grid, 0, 0, "Notoriety", String.valueOf(info.notoriety), "#" + info.notorietyRank);
        statRow(grid, 0, 18, "Wealth", String.valueOf(info.wealth), "#" + info.wealthRank);
        statRow(grid, 0, 36, "Legacy", String.valueOf(info.legacy), "#" + info.legacyRank);
        statRow(grid, 0, 54, "Total", String.valueOf(total), "#" + info.totalRank);
        statRow(grid, 148, 0, "Claims", String.valueOf(info.mNumClaims), claimLimit < 0 ? "INF" : String.valueOf(claimLimit));
        statRow(grid, 148, 18, "Members", String.valueOf(info.members.size()), "");
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            statRow(grid, 148, 36, "Citadel", "Lvl " + info.lvl, claimLimit < 0 ? "INF claims" : claimLimit + " claims");
        }
        statRow(grid, 148, 54, "Stash", String.valueOf(stashSlots), "slots");
        body.addChild(grid);

        UIElement actionRow = new UIElement();
        actionRow.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).width(rowWidth)
                .paddingTop(2).gapColumn(8).gapRow(ACTION_ROW_HEIGHT - BUTTON_HEIGHT));
        for (Action action : buttons) {
            actionRow.addChild(action.button());
        }
        actionSection.addChild(actionRow);

        show(root, mc);
    }

    private record Action(Button button, int width) {
    }

    private static Action action(String label, int width, Runnable run) {
        return new Action(actionButton(label, width, run), width);
    }

    private static List<Action> actionButtons(Minecraft mc, FactionDisplayInfo info) {
        List<Action> buttons = new ArrayList<>();
        PlayerDisplayInfo self = info.getPlayerInfo(mc.player.getUUID());
        boolean isOwnFaction = self != null;
        boolean isOfficer = self != null && self.role.ordinal() >= Faction.Role.OFFICER.ordinal();
        UUID displayedFaction = info.factionId;

        if (isOwnFaction) {
            buttons.add(action("Stash", 66, () -> FactionInsuranceGuiFactory.INSTANCE.openClientSibling(displayedFaction)));
            buttons.add(action("Members", 62, () -> FactionMemberManagerGuiFactory.INSTANCE.openClientSibling(FactionMemberManagerGuiData.Page.MEMBERS)));
            if (WarForgeConfig.ENABLE_CITADEL_UPGRADES && isOfficer) {
                buttons.add(action("Upgrade", 70, () -> FactionUpgradeGuiFactory.INSTANCE.openClientSibling(displayedFaction)));
            }
            buttons.add(action("Operations", 84, () -> OperationsGuiFactory.INSTANCE.openClient()));
            buttons.add(action("Flag", 66, () -> FactionFlagSelectGuiFactory.INSTANCE.openClient(displayedFaction)));
        }
        buttons.add(action("Refresh", 66, () -> {
            PacketRequestFactionInfo packet = new PacketRequestFactionInfo();
            packet.mFactionIDRequest = displayedFaction;
            WarForgeMod.NETWORK.sendToServer(packet);
        }));
        return buttons;
    }

    private static int countRows(List<Action> buttons, int rowWidth) {
        int rows = 1;
        int used = 0;
        for (Action action : buttons) {
            if (used > 0 && used + 8 + action.width() > rowWidth) {
                rows++;
                used = action.width();
            } else {
                used += used > 0 ? 8 + action.width() : action.width();
            }
        }
        return rows;
    }

    private static void statRow(UIElement grid, int x, int y, String label, String value, String extra) {
        UIElement row = place(grid, row(136, 16), x, y, 136, 16);
        row.addChild(width(text(label, TEXT_PRIMARY), 52));
        row.addChild(width(boldText(value, TEXT_PRIMARY), 42));
        row.addChild(width(text(extra, 0xBBBBBB), 42));
    }

    private static void show(UIElement root, Minecraft mc) {
        ClientCompat.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal("Faction Stats")));
    }
}
