package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.client.ui.model.DataRevision;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.client.util.PlayerFaceElement;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.network.PacketRequestFactionInfo;
import com.flansmod.warforge.common.network.PlayerDisplayInfo;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

@OnlyIn(Dist.CLIENT)
public final class FactionStatsScreen {
    private static final DataRevision REVISION = new DataRevision();
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
        if (info != null && REVISION.unchanged(ClientCompat.screen(), info)) {
            return;
        }

        boolean hasFaction = info != null && !info.factionId.equals(FactionIds.NONE);
        int sectionWidth = WIDTH - CONTENT_LEFT * 2;
        int rowWidth = sectionWidth - 10;

        List<Action> buttons = new ArrayList<>();
        if (hasFaction) {
            buttons = actionButtons(mc, info);
        }
        List<List<Action>> rows = splitRows(buttons, rowWidth);
        int height = BASE_HEIGHT + (rows.size() - 1) * ACTION_ROW_HEIGHT;

        UIElement root = screenRoot();
        UIElement panel = panel(WIDTH, height);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        UIElement body = section(panel, CONTENT_LEFT, BODY_Y, sectionWidth, 118);
        UIElement actionSection = section(panel, CONTENT_LEFT, ACTIONS_Y, sectionWidth, 34 + (rows.size() - 1) * ACTION_ROW_HEIGHT);
        colorStripe(panel, hasFaction ? info.colour : NO_FACTION_STRIPE, height);
        closeButton(panel, WIDTH).setOnClick(event -> ClientCompat.setScreen(null));

        title(panel, tr("warforge.ui.faction_stats"), TEXT_PRIMARY);

        if (!hasFaction) {
            subtitle(panel, text(tr("warforge.ui.no_faction_information_is_available"), TEXT_SECONDARY));
            show(root, mc);
            return;
        }

        subtitle(panel, text(info.factionName, info.colour));

        PlayerDisplayInfo leader = info.getPlayerInfo(info.leaderId);
        UIElement leaderRow = flow(FlexDirection.ROW);
        leaderRow.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).right(35).top(10).height(20).paddingHorizontal(2));
        PlayerFaceElement leaderFace = new PlayerFaceElement(info.leaderId);
        leaderFace.layout(l -> l.width(20).height(20).marginHorizontal(2).flexShrink(0));
        leaderRow.addChild(leaderFace);
        leaderRow.addChild(fit(text(tr("warforge.ui.leader_s", leader == null ? tr("warforge.ui.unknown") : leader.username), TEXT_SECONDARY)));
        panel.addChild(leaderRow);

        Label overview = fit(text(tr("warforge.ui.faction_overview"), TEXT_PRIMARY));
        overview.layout(l -> l.marginBottom(4));
        body.addChild(overview);
        Label blurb = wrap(text(tr("warforge.ui.standing_territory_membership_and_citadel_progre"), TEXT_MUTED), sectionWidth);
        blurb.layout(l -> l.marginBottom(6));
        body.addChild(blurb);

        int total = info.notoriety + info.wealth + info.legacy;
        int claimLimit = com.flansmod.warforge.client.ClientUpgradeTable.getClaimLimitForLevel(info.lvl);
        int stashSlots = com.flansmod.warforge.client.ClientUpgradeTable.getInsuranceSlotsForLevel(info.lvl);

        UIElement grid = box(rowWidth, 70);
        statRow(grid, 0, 0, tr("warforge.ui.notoriety"), String.valueOf(info.notoriety), "#" + info.notorietyRank);
        statRow(grid, 0, 18, tr("warforge.ui.wealth"), String.valueOf(info.wealth), "#" + info.wealthRank);
        statRow(grid, 0, 36, tr("warforge.ui.legacy"), String.valueOf(info.legacy), "#" + info.legacyRank);
        statRow(grid, 0, 54, tr("warforge.ui.total"), String.valueOf(total), "#" + info.totalRank);
        statRow(grid, 148, 0, tr("warforge.ui.claims"), String.valueOf(info.numClaims), claimLimit < 0 ? tr("warforge.ui.inf") : String.valueOf(claimLimit));
        statRow(grid, 148, 18, tr("warforge.ui.members"), String.valueOf(info.members.size()), "");
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            statRow(grid, 148, 36, tr("warforge.ui.citadel"), tr("warforge.ui.lvl_d", info.lvl), claimLimit < 0 ? tr("warforge.ui.inf_claims") : tr("warforge.ui.d_claims", claimLimit));
        }
        statRow(grid, 148, 54, tr("warforge.ui.stash"), String.valueOf(stashSlots), tr("warforge.ui.slots"));
        body.addChild(grid);

        for (int r = 0; r < rows.size(); r++) {
            UIElement actionRow = flow(FlexDirection.ROW);
            boolean first = r == 0;
            actionRow.layout(l -> l.width(rowWidth).height(first ? BUTTON_HEIGHT + 8 : ACTION_ROW_HEIGHT));
            List<Action> row = rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                Button button = row.get(i).button();
                if (i > 0) {
                    button.layout(l -> l.marginHorizontal(4));
                }
                actionRow.addChild(button);
            }
            actionSection.addChild(actionRow);
        }

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
        boolean isOfficer = self != null && self.role.ordinal() >= Role.OFFICER.ordinal();
        UUID displayedFaction = info.factionId;

        if (isOwnFaction) {
            buttons.add(action(tr("warforge.ui.stash"), 66, () -> UiNavigation.requestInsurance(displayedFaction)));
            buttons.add(action(tr("warforge.ui.members"), 62, () -> UiNavigation.requestMembers(FactionMemberManagerGuiData.Page.MEMBERS)));
            if (WarForgeConfig.ENABLE_CITADEL_UPGRADES && isOfficer) {
                buttons.add(action(tr("warforge.ui.upgrade"), 70, () -> UiNavigation.requestUpgrade(displayedFaction)));
            }
            buttons.add(action(tr("warforge.ui.operations"), 84, () -> UiNavigation.requestOperations()));
            buttons.add(action(tr("warforge.ui.flag"), 66, () -> UiNavigation.openFlagSelect(displayedFaction, info.factionName, info.colour, info.flagId)));
        }
        buttons.add(action(tr("warforge.ui.refresh"), 66, () -> {
            PacketRequestFactionInfo packet = new PacketRequestFactionInfo();
            packet.factionIdRequest = displayedFaction;
            WarForgeMod.NETWORK.sendToServer(packet);
        }));
        return buttons;
    }

    private static List<List<Action>> splitRows(List<Action> buttons, int rowWidth) {
        List<List<Action>> rows = new ArrayList<>();
        List<Action> row = new ArrayList<>();
        int used = 0;
        for (Action action : buttons) {
            int need = row.isEmpty() ? action.width() : action.width() + 8;
            if (!row.isEmpty() && used + need > rowWidth) {
                rows.add(row);
                row = new ArrayList<>();
                need = action.width();
                used = 0;
            }
            row.add(action);
            used += need;
        }
        rows.add(row);
        return rows;
    }

    private static void statRow(UIElement grid, int x, int y, String label, String value, String extra) {
        UIElement row = place(grid, row(136, 16), x, y, 136, 16);
        row.addChild(width(text(label, TEXT_PRIMARY), 52));
        row.addChild(width(text(value, TEXT_PRIMARY), 42));
        row.addChild(width(text(extra, TEXT_DIM), 42));
    }

    private static void show(UIElement root, Minecraft mc) {
        ModularUIScreen built = new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal(tr("warforge.ui.faction_stats")));
        REVISION.shown(built);
        ClientCompat.setScreen(built);
    }
}
