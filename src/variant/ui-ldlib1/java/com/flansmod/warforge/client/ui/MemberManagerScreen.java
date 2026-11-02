package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.client.ui.model.DataRevision;
import com.flansmod.warforge.client.ui.model.MemberManagerModel;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.ScrollArea;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.ui.model.UiText;
import com.flansmod.warforge.client.util.PlayerFaceElement;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.network.PacketFactionAllianceAction;
import com.flansmod.warforge.common.network.PacketFactionMemberManagerAction;
import com.flansmod.warforge.common.network.PacketMemberData;
import com.flansmod.warforge.common.network.PacketRequestMemberData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

@OnlyIn(Dist.CLIENT)
public final class MemberManagerScreen {
    private static final DataRevision REVISION = new DataRevision();
    private static final int WIDTH = 372;
    private static final int HEIGHT = 268;
    private static final int TAB_Y = 48;
    private static final int LIST_Y = 78;
    private static final int SECTION_WIDTH = WIDTH - CONTENT_LEFT * 2;
    private static final int LIST_SECTION_HEIGHT = HEIGHT - LIST_Y - 12;
    private static final int ROW_WIDTH = WIDTH - 44;
    private static final int ROW_HEIGHT = 24;
    private static final int TAB_WIDTH = 74;

    private MemberManagerScreen() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        PacketMemberData data = PacketMemberData.latest;
        if (data != null && REVISION.unchanged(Minecraft.getInstance().screen, data)) {
            return;
        }

        if (data == null) {
            return;
        }

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, WIDTH, HEIGHT);
        panel.addWidget(WarForgeUiTheme.header(WIDTH));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, TAB_Y, SECTION_WIDTH, 24));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, LIST_Y, SECTION_WIDTH, LIST_SECTION_HEIGHT));
        panel.addWidget(WarForgeUiTheme.stripe(data.hasFaction ? data.factionColor : WarForgeUiTheme.STRIPE_DEFAULT, HEIGHT));
        panel.addWidget(WarForgeUiTheme.closeButton(WIDTH, click -> ClientScreens.close()));
        panel.addWidget(WarForgeUiTheme.title(tr("warforge.ui.faction_members")));

        if (data.hasFaction) {
            Label name = Label.bold(data.factionName, data.factionColor).at(CONTENT_LEFT, WarForgeUiTheme.SUBTITLE_Y);
            panel.addWidget(name);
            Label separator = Label.text(" | ", WarForgeUiTheme.TEXT_SECONDARY)
                    .at(CONTENT_LEFT + name.getSizeWidth(), WarForgeUiTheme.SUBTITLE_Y);
            panel.addWidget(separator);
            panel.addWidget(Label.text(tr("warforge.ui.role_s", UiText.formatRole(data.viewerRole)), UiText.roleColor(data.viewerRole))
                    .at(CONTENT_LEFT + name.getSizeWidth() + separator.getSizeWidth(), WarForgeUiTheme.SUBTITLE_Y));
        } else {
            panel.addWidget(WarForgeUiTheme.subtitle(tr("warforge.ui.you_are_not_currently_in_a_faction"), WarForgeUiTheme.TEXT_SECONDARY, false));
        }

        int tabsWidth = TAB_WIDTH + 8 + TAB_WIDTH + 8 + (data.hasFaction ? TAB_WIDTH : 0);
        int tabX = CONTENT_LEFT + 5 + (SECTION_WIDTH - 10 - tabsWidth) / 2;
        int tabY = TAB_Y + 3;
        panel.addWidget(WarForgeUiTheme.at(tab(tr("warforge.ui.members"), data.page, FactionMemberManagerGuiData.Page.MEMBERS), tabX, tabY));
        panel.addWidget(WarForgeUiTheme.at(tab(tr("warforge.ui.invites"), data.page, FactionMemberManagerGuiData.Page.INVITES), tabX + TAB_WIDTH + 8, tabY));
        if (data.hasFaction) {
            panel.addWidget(WarForgeUiTheme.at(tab(tr("warforge.ui.alliances"), data.page, FactionMemberManagerGuiData.Page.ALLIANCES),
                    tabX + (TAB_WIDTH + 8) * 2, tabY));
        }

        int textX = CONTENT_LEFT + 5;
        if (MemberManagerModel.needsFaction(data)) {
            panel.addWidget(Label.text(tr("warforge.ui.join_or_create_a_faction_before_using_the_member"), WarForgeUiTheme.TEXT_NOTE)
                    .wrap(SECTION_WIDTH - 10).at(textX, LIST_Y + 5 + 6));
            REVISION.shown(ClientScreens.open(panel));
            return;
        }

        int y = LIST_Y + 5;
        panel.addWidget(Label.bold(MemberManagerModel.sectionTitle(data), WarForgeUiTheme.TEXT_MUTED).at(textX, y));
        y += 9 + 4;
        Label description = Label.text(MemberManagerModel.sectionDescription(data), WarForgeUiTheme.TEXT_MUTED).wrap(SECTION_WIDTH - 10).at(textX, y);
        panel.addWidget(description);
        y += description.getSizeHeight() + 6;

        boolean alliancePage = data.page == FactionMemberManagerGuiData.Page.ALLIANCES;
        if (alliancePage) {
            String label = MemberManagerModel.allyAccessLabel(data.allowAllyInteraction);
            panel.addWidget(WarForgeUiTheme.at(WarForgeUiTheme.actionButton(label, 160, data.canManageAlliances,
                    click -> sendAlliance(PacketFactionAllianceAction.Action.TOGGLE_ALLY_BUILD, data.factionId, data.page)), textX, y));
            y += WarForgeUiTheme.BUTTON_HEIGHT + 5;
        }

        int listBottom = LIST_Y + LIST_SECTION_HEIGHT - 5;
        ScrollArea list = new ScrollArea(textX, y, SECTION_WIDTH - 10, Math.max(24, listBottom - y));

        String empty = MemberManagerModel.emptyMessage(data);
        if (empty != null) {
            list.addRow(emptyText(empty), 6, 6, 0);
        } else if (data.page == FactionMemberManagerGuiData.Page.MEMBERS) {
            for (FactionMemberManagerGuiData.MemberEntry member : data.members) {
                list.addRow(memberRow(member, data.page), 0, 0, 2);
            }
        } else if (alliancePage) {
            byte lastKind = -1;
            for (FactionMemberManagerGuiData.AllianceEntry entry : data.alliances) {
                if (entry.kind != lastKind) {
                    list.addRow(allianceHeader(entry.kind), 2, 4, 2);
                    lastKind = entry.kind;
                }
                list.addRow(allianceRow(entry, data.canManageAlliances, data.page), 0, 0, 2);
            }
        } else {
            for (FactionMemberManagerGuiData.InviteEntry invite : data.inviteCandidates) {
                list.addRow(data.hasFaction ? inviteRow(invite, data.page) : incomingInviteRow(invite, data.page), 0, 0, 2);
            }
        }
        panel.addWidget(list.finish());

        REVISION.shown(ClientScreens.open(panel));
    }

    private static ButtonWidget tab(String label, FactionMemberManagerGuiData.Page current, FactionMemberManagerGuiData.Page page) {
        return WarForgeUiTheme.tabButton(label, TAB_WIDTH, current == page, click -> requestPage(page));
    }

    private static void requestPage(FactionMemberManagerGuiData.Page page) {
        PacketRequestMemberData request = new PacketRequestMemberData();
        request.page = page;
        WarForgeMod.NETWORK.sendToServer(request);
    }

    private static Label emptyText(String text) {
        return Label.text(text, WarForgeUiTheme.TEXT_MUTED);
    }

    private static WidgetGroup rowFrame() {
        WidgetGroup row = new WidgetGroup(0, 0, ROW_WIDTH, ROW_HEIGHT);
        row.setBackground(WarForgeUiTheme.insetBackdrop(WarForgeUiTheme.ROW_FILL));
        return row;
    }

    private static void layoutRow(WidgetGroup row, List<Widget> children, List<Integer> margins) {
        int x = 3;
        for (int i = 0; i < children.size(); i++) {
            Widget child = children.get(i);
            int margin = margins.get(i);
            child.setSelfPosition(x + margin, (ROW_HEIGHT - child.getSizeHeight()) / 2);
            row.addWidget(child);
            x += margin + child.getSizeWidth() + margin;
        }
    }

    private static Label cellText(String text, int rgb, int width) {
        return Label.text(text, rgb).box(width, ROW_HEIGHT - 6);
    }

    private static WidgetGroup memberRow(FactionMemberManagerGuiData.MemberEntry member, FactionMemberManagerGuiData.Page page) {
        int rankColor = UiText.roleColor(member.role);

        List<ButtonWidget> buttons = new ArrayList<>();
        if (member.canTransferLeadership) {
            buttons.add(memberActionButton(tr("warforge.ui.lead"), 36, PacketFactionMemberManagerAction.Action.TRANSFER_LEADER, member.playerId, page));
        }
        if (member.canPromote) {
            buttons.add(memberActionButton(tr("warforge.ui.promote"), 54, PacketFactionMemberManagerAction.Action.PROMOTE, member.playerId, page));
        }
        if (member.canDemote) {
            buttons.add(memberActionButton(tr("warforge.ui.demote"), 52, PacketFactionMemberManagerAction.Action.DEMOTE, member.playerId, page));
        }
        if (member.canKickOrLeave) {
            buttons.add(memberActionButton(member.self ? tr("warforge.ui.leave") : tr("warforge.ui.kick"), 42, PacketFactionMemberManagerAction.Action.KICK_OR_LEAVE, member.playerId, page));
        }
        int buttonsWidth = buttons.stream().mapToInt(Widget::getSizeWidth).sum();
        int free = ROW_WIDTH - 3 - (18 + 5 + 96 + 5 + 52 + 44) - Math.max(24, buttonsWidth) - 3;
        int nameWidth = Math.max(40, 96 + Math.min(0, free));

        List<Widget> children = new ArrayList<>();
        List<Integer> margins = new ArrayList<>();
        children.add(new PlayerFaceElement(member.playerId, 18));
        margins.add(0);
        children.add(cellText(member.username, rankColor, nameWidth).tooltip(member.username));
        margins.add(5);
        children.add(cellText(UiText.formatRole(member.role), rankColor, 52));
        margins.add(0);
        children.add(cellText(member.online ? tr("warforge.ui.online") : tr("warforge.ui.offline"), member.online ? WarForgeUiTheme.TEXT_SUCCESS : WarForgeUiTheme.TEXT_OFFLINE, 44));
        margins.add(0);
        for (ButtonWidget button : buttons) {
            children.add(button);
            margins.add(0);
        }
        if (buttons.isEmpty()) {
            children.add(cellText("-", WarForgeUiTheme.TEXT_PRIMARY, 24));
            margins.add(0);
        }

        WidgetGroup row = rowFrame();
        layoutRow(row, children, margins);
        return row;
    }

    private static WidgetGroup inviteRow(FactionMemberManagerGuiData.InviteEntry invite, FactionMemberManagerGuiData.Page page) {
        List<Widget> children = new ArrayList<>();
        List<Integer> margins = new ArrayList<>();
        children.add(new PlayerFaceElement(invite.playerId, 18));
        margins.add(0);
        children.add(cellText(invite.username, WarForgeUiTheme.TEXT_PRIMARY, 178).tooltip(invite.username));
        margins.add(5);
        children.add(cellText(invite.invited ? tr("warforge.ui.pending") : tr("warforge.ui.available"), invite.invited ? WarForgeUiTheme.TEXT_PENDING : WarForgeUiTheme.TEXT_SUCCESS, 64));
        margins.add(0);
        children.add(WarForgeUiTheme.actionButton(invite.invited ? tr("warforge.ui.invited") : tr("warforge.ui.invite"), 54, invite.canInvite, click -> {
            PacketFactionMemberManagerAction packet = new PacketFactionMemberManagerAction();
            packet.action = PacketFactionMemberManagerAction.Action.INVITE;
            packet.target = invite.playerId;
            WarForgeMod.NETWORK.sendToServer(packet);
        }));
        margins.add(0);

        WidgetGroup row = rowFrame();
        layoutRow(row, children, margins);
        return row;
    }

    private static WidgetGroup incomingInviteRow(FactionMemberManagerGuiData.InviteEntry invite, FactionMemberManagerGuiData.Page page) {
        List<Widget> children = new ArrayList<>();
        List<Integer> margins = new ArrayList<>();
        if (!invite.inviterId.equals(FactionIds.NONE)) {
            children.add(new PlayerFaceElement(invite.inviterId, 18));
        } else {
            children.add(cellText("?", WarForgeUiTheme.TEXT_MUTED, 18));
        }
        margins.add(0);
        children.add(cellText(invite.username, invite.factionColor & 0xFFFFFF, 108).tooltip(invite.username));
        margins.add(5);
        Label from = cellText(invite.inviterName.isEmpty() ? tr("warforge.ui.faction_invite") : tr("warforge.ui.from_s", invite.inviterName), WarForgeUiTheme.TEXT_SECONDARY, 116);
        if (!invite.inviterName.isEmpty()) {
            from.tooltip(tr("warforge.ui.invited_by_s", invite.inviterName));
        }
        children.add(from);
        margins.add(0);
        UUID factionId = invite.factionId;
        children.add(WarForgeUiTheme.actionButton(tr("warforge.ui.join"), 44, invite.canAccept, click -> {
            PacketFactionMemberManagerAction packet = new PacketFactionMemberManagerAction();
            packet.action = PacketFactionMemberManagerAction.Action.ACCEPT_INVITE;
            packet.target = factionId;
            WarForgeMod.NETWORK.sendToServer(packet);
        }));
        margins.add(0);

        WidgetGroup row = rowFrame();
        layoutRow(row, children, margins);
        return row;
    }

    private static Label allianceHeader(byte kind) {
        return Label.text(MemberManagerModel.allianceHeader(kind), WarForgeUiTheme.TEXT_MUTED);
    }

    private static WidgetGroup allianceRow(FactionMemberManagerGuiData.AllianceEntry entry, boolean canManage, FactionMemberManagerGuiData.Page page) {
        List<Widget> children = new ArrayList<>();
        List<Integer> margins = new ArrayList<>();
        children.add(WarForgeUiTheme.box(0, 0, 18, 18, WarForgeUiTheme.solid(entry.factionColor)));
        margins.add(0);
        children.add(cellText(entry.factionName, entry.factionColor & 0xFFFFFF, 128).tooltip(entry.factionName));
        margins.add(5);

        if (entry.kind == FactionMemberManagerGuiData.AllianceEntry.KIND_PENDING) {
            children.add(cellText(tr("warforge.ui.requested"), WarForgeUiTheme.TEXT_PENDING, 58));
            margins.add(0);
            children.add(allianceActionButton(tr("warforge.ui.accept"), 50, canManage, PacketFactionAllianceAction.Action.ACCEPT, entry.factionId, page));
            margins.add(0);
            children.add(allianceActionButton(tr("warforge.ui.decline"), 54, canManage, PacketFactionAllianceAction.Action.DECLINE, entry.factionId, page));
            margins.add(4);
        } else {
            children.add(cellText(tr("warforge.ui.d_online", entry.onlineCount), entry.onlineCount > 0 ? WarForgeUiTheme.TEXT_SUCCESS : WarForgeUiTheme.TEXT_OFFLINE, 58));
            margins.add(0);
            if (entry.kind == FactionMemberManagerGuiData.AllianceEntry.KIND_ALLY) {
                if (canManage) {
                    children.add(WarForgeUiTheme.dangerButton(tr("warforge.ui.break"), 52,
                            click -> sendAlliance(PacketFactionAllianceAction.Action.BREAK, entry.factionId, page)));
                } else {
                    children.add(cellText("-", WarForgeUiTheme.TEXT_MUTED, 52));
                }
            } else {
                children.add(allianceActionButton(tr("warforge.ui.invite"), 52, canManage, PacketFactionAllianceAction.Action.INVITE, entry.factionId, page));
            }
            margins.add(0);
        }

        WidgetGroup row = rowFrame();
        layoutRow(row, children, margins);
        return row;
    }

    private static ButtonWidget memberActionButton(String label, int width, PacketFactionMemberManagerAction.Action action, UUID target, FactionMemberManagerGuiData.Page page) {
        return WarForgeUiTheme.actionButton(label, width, click -> {
            PacketFactionMemberManagerAction packet = new PacketFactionMemberManagerAction();
            packet.action = action;
            packet.target = target;
            WarForgeMod.NETWORK.sendToServer(packet);
        });
    }

    private static ButtonWidget allianceActionButton(String label, int width, boolean enabled, PacketFactionAllianceAction.Action action, UUID target, FactionMemberManagerGuiData.Page page) {
        return WarForgeUiTheme.actionButton(label, width, enabled, click -> sendAlliance(action, target, page));
    }

    private static void sendAlliance(PacketFactionAllianceAction.Action action, UUID target, FactionMemberManagerGuiData.Page page) {
        PacketFactionAllianceAction packet = new PacketFactionAllianceAction();
        packet.action = action;
        packet.target = target;
        WarForgeMod.NETWORK.sendToServer(packet);
    }
}
