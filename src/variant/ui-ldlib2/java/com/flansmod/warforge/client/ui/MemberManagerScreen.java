package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.client.ui.model.UiText;
import com.flansmod.warforge.client.util.PlayerFaceElement;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.network.PacketFactionAllianceAction;
import com.flansmod.warforge.common.network.PacketFactionMemberManagerAction;
import com.flansmod.warforge.common.network.PacketMemberData;
import com.flansmod.warforge.common.network.PacketRequestMemberData;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.AlignContent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

/** Faction members, invites and alliances, laid out like GuiFactionMemberManager of the 1.20.1 build. */
@OnlyIn(Dist.CLIENT)
public final class MemberManagerScreen {
    private static final int WIDTH = 372;
    private static final int HEIGHT = 268;
    private static final int TAB_Y = 48;
    private static final int LIST_Y = 78;
    private static final int ROW_WIDTH = WIDTH - 44;

    private MemberManagerScreen() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        PacketMemberData data = PacketMemberData.latest;
        if (data == null) {
            return;
        }

        int sectionWidth = WIDTH - CONTENT_LEFT * 2;
        int listSectionHeight = HEIGHT - LIST_Y - 12;
        int headerWidth = WIDTH - CONTENT_LEFT * 2 - 18;

        UIElement root = screenRoot(0);
        UIElement panel = panel(WIDTH, HEIGHT);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        UIElement tabSection = section(panel, CONTENT_LEFT, TAB_Y, sectionWidth, 24);
        tabSection.layout(l -> l.paddingVertical(3).justifyContent(AlignContent.CENTER));
        UIElement listSection = section(panel, CONTENT_LEFT, LIST_Y, sectionWidth, listSectionHeight);
        colorStripe(panel, data.hasFaction ? data.factionColor : NO_FACTION_STRIPE, HEIGHT);
        closeButton(panel, WIDTH).setOnClick(event -> ClientCompat.setScreen(null));

        title(panel, "Faction Members", TEXT_PRIMARY, headerWidth);
        if (data.hasFaction) {
            UIElement headerRow = place(panel, row(headerWidth, 12), CONTENT_LEFT, SUBTITLE_Y, headerWidth, 12);
            headerRow.layout(l -> l.alignItems(dev.vfyjxf.taffy.style.AlignItems.START));
            Label name = boldText(data.factionName, data.factionColor);
            Label separator = text(" | ", TEXT_SECONDARY);
            Label role = text("Role: " + UiText.formatRole(data.viewerRole), UiText.roleColor(data.viewerRole));
            headerRow.addChild(width(name, mc.font.width(name.getValue()) + 1));
            headerRow.addChild(width(separator, mc.font.width(separator.getValue())));
            headerRow.addChild(width(role, mc.font.width(role.getValue()) + 1));
        } else {
            subtitle(panel, text("You are not currently in a faction", TEXT_SECONDARY), headerWidth);
        }

        UIElement tabRow = row(sectionWidth - 10, BUTTON_HEIGHT);
        tabRow.addChild(tabButton("Members", data.page == FactionMemberManagerGuiData.Page.MEMBERS, FactionMemberManagerGuiData.Page.MEMBERS));
        Button invitesTab = tabButton("Invites", data.page == FactionMemberManagerGuiData.Page.INVITES, FactionMemberManagerGuiData.Page.INVITES);
        invitesTab.layout(l -> l.marginHorizontal(8));
        tabRow.addChild(invitesTab);
        if (data.hasFaction) {
            tabRow.addChild(tabButton("Alliances", data.page == FactionMemberManagerGuiData.Page.ALLIANCES, FactionMemberManagerGuiData.Page.ALLIANCES));
        }
        tabSection.addChild(tabRow);

        if (!data.hasFaction && data.page != FactionMemberManagerGuiData.Page.INVITES) {
            Label info = width(text("Join or create a faction before using the member console.", TEXT_INFO), sectionWidth - 10);
            info.layout(l -> l.marginVertical(6));
            listSection.addChild(info);
            show(root, mc);
            return;
        }

        String sectionTitle = switch (data.page) {
            case MEMBERS -> "Roster";
            case ALLIANCES -> "Alliances";
            default -> data.hasFaction ? "Invite Console" : "Pending Invites";
        };
        String sectionDescription = switch (data.page) {
            case MEMBERS -> "Faces, rank, presence, and direct faction actions.";
            case ALLIANCES -> "Ally with factions to stop sieges between you. Toggle whether allies may use your land.";
            default -> data.hasFaction
                    ? "Invite online unaffiliated players into the faction."
                    : "Accept one of your outstanding faction invites.";
        };

        Label titleLabel = width(boldText(sectionTitle, TEXT_MUTED), sectionWidth - 10);
        titleLabel.layout(l -> l.marginBottom(4));
        listSection.addChild(titleLabel);
        Label description = scrollingText(literal(sectionDescription, TEXT_MUTED, false), sectionWidth - 10);
        description.layout(l -> l.marginBottom(6));
        listSection.addChild(tooltip(description, sectionDescription));

        boolean alliancePage = data.page == FactionMemberManagerGuiData.Page.ALLIANCES;
        if (alliancePage) {
            Button allyToggle = actionButton("Ally Access: " + (data.allowAllyInteraction ? "ENABLED" : "DISABLED"),
                    160, data.canManageAlliances,
                    () -> sendAlliance(PacketFactionAllianceAction.Action.TOGGLE_ALLY_BUILD, data.factionId, data.page));
            allyToggle.layout(l -> l.marginBottom(5));
            listSection.addChild(allyToggle);
        }

        ScrollerView list = insetList(sectionWidth - 10, listSectionHeight - 46 - (alliancePage ? 23 : 0));

        if (data.page == FactionMemberManagerGuiData.Page.MEMBERS) {
            if (data.members.isEmpty()) {
                list.addScrollViewChild(emptyLine("No faction members found."));
            } else {
                for (FactionMemberManagerGuiData.MemberEntry member : data.members) {
                    list.addScrollViewChild(memberRow(member, data.page));
                }
            }
        } else if (alliancePage) {
            if (data.alliances.isEmpty()) {
                list.addScrollViewChild(emptyLine("No factions available to ally with yet."));
            } else {
                byte lastKind = -1;
                for (FactionMemberManagerGuiData.AllianceEntry entry : data.alliances) {
                    if (entry.kind != lastKind) {
                        list.addScrollViewChild(allianceHeader(entry.kind));
                        lastKind = entry.kind;
                    }
                    list.addScrollViewChild(allianceRow(entry, data.canManageAlliances, data.page));
                }
            }
        } else {
            if (!data.hasFaction) {
                if (data.inviteCandidates.isEmpty()) {
                    list.addScrollViewChild(emptyLine("You have no open faction invites."));
                } else {
                    for (FactionMemberManagerGuiData.InviteEntry invite : data.inviteCandidates) {
                        list.addScrollViewChild(incomingInviteRow(invite, data.page));
                    }
                }
            } else if (!data.canInvitePlayers) {
                list.addScrollViewChild(emptyLine("Officer or leader rank is required to send invites."));
            } else if (data.inviteCandidates.isEmpty()) {
                list.addScrollViewChild(emptyLine("No online players are currently eligible for invite."));
            } else {
                for (FactionMemberManagerGuiData.InviteEntry invite : data.inviteCandidates) {
                    list.addScrollViewChild(inviteRow(invite, data.page));
                }
            }
        }

        listSection.addChild(list);
        show(root, mc);
    }

    private static void show(UIElement root, Minecraft mc) {
        ClientCompat.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal("Members")));
    }

    private static UIElement emptyLine(String message) {
        UIElement line = box(ROW_WIDTH, 21);
        place(line, text(message, TEXT_PRIMARY), 6, 6, ROW_WIDTH - 6, 9);
        return line;
    }

    private static Label nameText(String value, int color, int width, String tooltip) {
        Label label = scrollingText(literal(value, color, false), width);
        label.layout(l -> l.marginHorizontal(5));
        if (tooltip != null) {
            tooltip(label, tooltip);
        }
        return label;
    }

    private static UIElement face(UUID playerId) {
        PlayerFaceElement face = new PlayerFaceElement(playerId);
        face.layout(l -> l.width(18).height(18).flexShrink(0));
        return face;
    }

    private static UIElement memberRow(FactionMemberManagerGuiData.MemberEntry member, FactionMemberManagerGuiData.Page page) {
        UIElement row = listRow(ROW_WIDTH, 24);
        int rankColor = UiText.roleColor(member.role);

        row.addChild(face(member.playerId));
        row.addChild(nameText(member.username, rankColor, 96, member.username));
        row.addChild(width(text(UiText.formatRole(member.role), rankColor), 52));
        row.addChild(width(text(member.online ? "Online" : "Offline", member.online ? TEXT_SUCCESS : 0xAAAAAA), 44));

        if (member.canTransferLeadership) {
            row.addChild(memberActionButton("Lead", 36, true, PacketFactionMemberManagerAction.Action.TRANSFER_LEADER, member.playerId, page));
        }
        if (member.canPromote) {
            row.addChild(memberActionButton("Promote", 54, true, PacketFactionMemberManagerAction.Action.PROMOTE, member.playerId, page));
        }
        if (member.canDemote) {
            row.addChild(memberActionButton("Demote", 52, true, PacketFactionMemberManagerAction.Action.DEMOTE, member.playerId, page));
        }
        if (member.canKickOrLeave) {
            row.addChild(memberActionButton(member.self ? "Leave" : "Kick", 42, true, PacketFactionMemberManagerAction.Action.KICK_OR_LEAVE, member.playerId, page));
        }
        if (!member.canTransferLeadership && !member.canPromote && !member.canDemote && !member.canKickOrLeave) {
            row.addChild(width(text("-", TEXT_PRIMARY), 24));
        }
        return row;
    }

    private static UIElement inviteRow(FactionMemberManagerGuiData.InviteEntry invite, FactionMemberManagerGuiData.Page page) {
        UIElement row = listRow(ROW_WIDTH, 24);
        row.addChild(face(invite.playerId));
        row.addChild(nameText(invite.username, TEXT_PRIMARY, 178, invite.username));
        row.addChild(width(text(invite.invited ? "Pending" : "Available", invite.invited ? 0xFFAA00 : TEXT_SUCCESS), 64));
        row.addChild(memberActionButton(invite.invited ? "Invited" : "Invite", 54, invite.canInvite,
                PacketFactionMemberManagerAction.Action.INVITE, invite.playerId, page));
        return row;
    }

    private static UIElement incomingInviteRow(FactionMemberManagerGuiData.InviteEntry invite, FactionMemberManagerGuiData.Page page) {
        UIElement row = listRow(ROW_WIDTH, 24);
        if (!invite.inviterId.equals(Faction.nullUuid)) {
            row.addChild(face(invite.inviterId));
        } else {
            row.addChild(width(text("?", TEXT_MUTED), 18));
        }
        row.addChild(nameText(invite.username, invite.factionColor & 0xFFFFFF, 108, invite.username));
        String fromText = invite.inviterName.isEmpty() ? "Faction invite" : "From: " + invite.inviterName;
        Label from = scrollingText(literal(fromText, TEXT_SECONDARY, false), 116);
        if (!invite.inviterName.isEmpty()) {
            tooltip(from, "Invited by " + invite.inviterName);
        }
        row.addChild(from);
        row.addChild(memberActionButton("Join", 44, invite.canAccept,
                PacketFactionMemberManagerAction.Action.ACCEPT_INVITE, invite.factionId, page));
        return row;
    }

    private static UIElement allianceHeader(byte kind) {
        String label = switch (kind) {
            case FactionMemberManagerGuiData.AllianceEntry.KIND_ALLY -> "Current Allies";
            case FactionMemberManagerGuiData.AllianceEntry.KIND_PENDING -> "Incoming Requests";
            default -> "Invite a Faction";
        };
        Label header = width(text(label, TEXT_MUTED), ROW_WIDTH - 2);
        header.layout(l -> l.marginLeft(2).marginTop(4).marginBottom(2));
        return header;
    }

    private static UIElement allianceRow(FactionMemberManagerGuiData.AllianceEntry entry, boolean canManage, FactionMemberManagerGuiData.Page page) {
        UIElement row = listRow(ROW_WIDTH, 24);
        row.addChild(swatch(entry.factionColor, 18));
        row.addChild(nameText(entry.factionName, entry.factionColor & 0xFFFFFF, 128, entry.factionName));

        if (entry.kind == FactionMemberManagerGuiData.AllianceEntry.KIND_PENDING) {
            row.addChild(width(text("Requested", 0xFFAA00), 58));
            row.addChild(allianceActionButton("Accept", 50, canManage, PacketFactionAllianceAction.Action.ACCEPT, entry.factionId, page));
            Button decline = allianceActionButton("Decline", 54, canManage, PacketFactionAllianceAction.Action.DECLINE, entry.factionId, page);
            decline.layout(l -> l.marginHorizontal(4));
            row.addChild(decline);
        } else {
            row.addChild(width(text(entry.onlineCount + " online", entry.onlineCount > 0 ? TEXT_SUCCESS : 0xAAAAAA), 58));
            if (entry.kind == FactionMemberManagerGuiData.AllianceEntry.KIND_ALLY) {
                if (canManage) {
                    row.addChild(dangerButton("Break", 52, () -> sendAlliance(PacketFactionAllianceAction.Action.BREAK, entry.factionId, page)));
                } else {
                    row.addChild(width(text("-", TEXT_MUTED), 52));
                }
            } else {
                row.addChild(allianceActionButton("Invite", 52, canManage, PacketFactionAllianceAction.Action.INVITE, entry.factionId, page));
            }
        }
        return row;
    }

    private static Button tabButton(String label, boolean selected, FactionMemberManagerGuiData.Page page) {
        return com.flansmod.warforge.api.modularui.WarForgeUiTheme.tabButton(label, 74, selected, () -> {
            PacketRequestMemberData request = new PacketRequestMemberData();
            request.page = page;
            WarForgeMod.NETWORK.sendToServer(request);
        });
    }

    private static Button memberActionButton(String label, int width, boolean enabled, PacketFactionMemberManagerAction.Action action, UUID target, FactionMemberManagerGuiData.Page page) {
        return actionButton(label, width, enabled, () -> {
            PacketFactionMemberManagerAction packet = new PacketFactionMemberManagerAction();
            packet.action = action;
            packet.target = target;
            packet.page = page;
            WarForgeMod.NETWORK.sendToServer(packet);
        });
    }

    private static Button allianceActionButton(String label, int width, boolean enabled, PacketFactionAllianceAction.Action action, UUID target, FactionMemberManagerGuiData.Page page) {
        return actionButton(label, width, enabled, () -> sendAlliance(action, target, page));
    }

    private static void sendAlliance(PacketFactionAllianceAction.Action action, UUID target, FactionMemberManagerGuiData.Page page) {
        PacketFactionAllianceAction packet = new PacketFactionAllianceAction();
        packet.action = action;
        packet.target = target;
        packet.page = page;
        WarForgeMod.NETWORK.sendToServer(packet);
    }
}
