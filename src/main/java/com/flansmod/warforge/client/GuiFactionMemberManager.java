package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.flansmod.warforge.client.util.PlayerFaceDrawable;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiFactory;
import com.flansmod.warforge.common.factories.FactionStatsGuiFactory;
import com.flansmod.warforge.common.network.PacketFactionMemberManagerAction;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.TextFormatting;

public final class GuiFactionMemberManager {
    private static final int WIDTH = 372;
    private static final int HEIGHT = 268;
    private static final int CONTENT_LEFT = 12;
    private static final int HEADER_Y = 12;
    private static final int TAB_Y = 48;
    private static final int LIST_Y = 78;

    private GuiFactionMemberManager() {
    }

    public static ModularPanel buildPanel(FactionMemberManagerGuiData data) {
        ModularPanel panel = ModularPanel.defaultPanel("faction_member_manager")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(0, 0, WIDTH, 40, 0xFF171B1F, 0xFF0D1013)).size(WIDTH, 40));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, TAB_Y, WIDTH - CONTENT_LEFT * 2, 24, 0xEE20262B, 0xEE11161A)).size(WIDTH - CONTENT_LEFT * 2, 24).pos(CONTENT_LEFT, TAB_Y));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, LIST_Y, WIDTH - CONTENT_LEFT * 2, HEIGHT - LIST_Y - 12, 0xEE20262B, 0xEE11161A)).size(WIDTH - CONTENT_LEFT * 2, HEIGHT - LIST_Y - 12).pos(CONTENT_LEFT, LIST_Y));
        panel.child(new IDrawable.DrawableWidget(colorStripe(data.hasFaction ? 0xFF000000 | (0x00FFFFFF & 0x4E8E87) : 0xFF4A4A4A, 0, 0, 6, HEIGHT)).size(6, HEIGHT));
        panel.child(ButtonWidget.panelCloseButton().pos(WIDTH - 18, 8));

        panel.child(IKey.str(data.hasFaction ? "Faction Members" : "Faction Members").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y)
                .style(TextFormatting.BOLD)
                .shadow(true)
                .scale(1.15f));
        panel.child(IKey.str(data.hasFaction ? data.factionName + " | Role: " + formatRole(data.viewerRole) : "You are not currently in a faction").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y + 15)
                .color(0xC7CCD1));

        if (data.hasFaction) {
            panel.child(new ButtonWidget<>()
                    .width(58)
                    .height(18)
                    .overlay(IKey.str("Stats"))
                    .background(GuiTextures.MC_BUTTON)
                    .hoverBackground(GuiTextures.MC_BUTTON_HOVERED)
                    .onMousePressed(mouseButton -> {
                        FactionStatsGuiFactory.INSTANCE.openClient(data.factionId);
                        return true;
                    })
                    .pos(WIDTH - 80, 11));
        }

        panel.child(tabButton("Members", CONTENT_LEFT + 8, TAB_Y + 3, data.page == FactionMemberManagerGuiData.Page.MEMBERS, FactionMemberManagerGuiData.Page.MEMBERS));
        panel.child(tabButton("Invites", CONTENT_LEFT + 90, TAB_Y + 3, data.page == FactionMemberManagerGuiData.Page.INVITES, FactionMemberManagerGuiData.Page.INVITES));

        if (!data.hasFaction) {
            panel.child(IKey.str("Join or create a faction before using the member console.").asWidget()
                    .pos(CONTENT_LEFT + 12, LIST_Y + 14)
                    .color(0xD5D9DE));
            return panel;
        }

        panel.child(IKey.str(data.page == FactionMemberManagerGuiData.Page.MEMBERS ? "Roster" : "Invite Console").asWidget()
                .pos(CONTENT_LEFT + 10, LIST_Y + 8)
                .style(TextFormatting.BOLD));
        panel.child(IKey.str(data.page == FactionMemberManagerGuiData.Page.MEMBERS
                        ? "Faces, rank, presence, and direct faction actions."
                        : "Invite online unaffiliated players into the faction.")
                .asWidget()
                .pos(CONTENT_LEFT + 10, LIST_Y + 20)
                .color(0xB8BDC3));

        ListWidget list = new ListWidget<>()
                .scrollDirection(GuiAxis.Y)
                .background(GuiTextures.SLOT_ITEM)
                .width(WIDTH - 26)
                .height(HEIGHT - LIST_Y - 46)
                .pos(CONTENT_LEFT + 7, LIST_Y + 38);

        if (data.page == FactionMemberManagerGuiData.Page.MEMBERS) {
            if (data.members.isEmpty()) {
                list.addChild(IKey.str("No faction members found.").asWidget().pos(6, 6), 0);
            } else {
                int index = 0;
                for (FactionMemberManagerGuiData.MemberEntry member : data.members) {
                    list.addChild(createMemberRow(member, data.page), index++);
                }
            }
        } else {
            if (!data.canInvitePlayers) {
                list.addChild(IKey.str("Officer or leader rank is required to send invites.").asWidget().pos(6, 6), 0);
            } else if (data.inviteCandidates.isEmpty()) {
                list.addChild(IKey.str("No online players are currently eligible for invite.").asWidget().pos(6, 6), 0);
            } else {
                int index = 0;
                for (FactionMemberManagerGuiData.InviteEntry invite : data.inviteCandidates) {
                    list.addChild(createInviteRow(invite, data.page), index++);
                }
            }
        }

        panel.child(list);
        return panel;
    }

    private static Widget createMemberRow(FactionMemberManagerGuiData.MemberEntry member, FactionMemberManagerGuiData.Page page) {
        Row row = new Row();
        row.width(WIDTH - 44);
        row.height(24);
        row.mainAxisAlignment(Alignment.MainAxis.START);

        String status = member.online ? "Online" : "Offline";
        row.child(new IDrawable.DrawableWidget(new PlayerFaceDrawable(member.playerId)).size(18, 18));
        row.child(new ScrollingTextWidget(IKey.str(member.username))
                .width(96)
                .tooltip(tooltip -> tooltip.addLine(member.username)));
        row.child(IKey.str(formatRole(member.role)).asWidget().width(52));
        row.child(IKey.str(status).color(member.online ? 0x55FF55 : 0xAAAAAA).asWidget().width(44));

        if (member.canTransferLeadership) {
            row.child(actionButton("Lead", 36, true, PacketFactionMemberManagerAction.Action.TRANSFER_LEADER, member.playerId, page));
        }
        if (member.canPromote) {
            row.child(actionButton("Promote", 54, true, PacketFactionMemberManagerAction.Action.PROMOTE, member.playerId, page));
        }
        if (member.canDemote) {
            row.child(actionButton("Demote", 52, true, PacketFactionMemberManagerAction.Action.DEMOTE, member.playerId, page));
        }
        if (member.canKickOrLeave) {
            row.child(actionButton(member.self ? "Leave" : "Kick", 42, true, PacketFactionMemberManagerAction.Action.KICK_OR_LEAVE, member.playerId, page));
        }

        if (!member.canTransferLeadership && !member.canPromote && !member.canDemote && !member.canKickOrLeave) {
            row.child(IKey.str("-").asWidget().width(24));
        }

        return row;
    }

    private static Widget createInviteRow(FactionMemberManagerGuiData.InviteEntry invite, FactionMemberManagerGuiData.Page page) {
        Row row = new Row();
        row.width(WIDTH - 44);
        row.height(24);
        row.mainAxisAlignment(Alignment.MainAxis.START);

        row.child(new IDrawable.DrawableWidget(new PlayerFaceDrawable(invite.playerId)).size(18, 18));
        row.child(new ScrollingTextWidget(IKey.str(invite.username))
                .width(178)
                .tooltip(tooltip -> tooltip.addLine(invite.username)));
        row.child(IKey.str(invite.invited ? "Pending" : "Available").color(invite.invited ? 0xFFAA00 : 0x55FF55).asWidget().width(64));
        row.child(actionButton(invite.invited ? "Invited" : "Invite", 54, invite.canInvite, PacketFactionMemberManagerAction.Action.INVITE, invite.playerId, page));
        return row;
    }

    private static ButtonWidget<?> actionButton(String label, int width, boolean enabled, PacketFactionMemberManagerAction.Action action, java.util.UUID target, FactionMemberManagerGuiData.Page page) {
        return new ButtonWidget<>()
                .width(width)
                .height(16)
                .overlay(IKey.str(label).color(enabled ? 0xFFFFFF : 0x666666))
                .background(enabled ? GuiTextures.MC_BUTTON : GuiTextures.MC_BUTTON_DISABLED)
                .hoverBackground(enabled ? GuiTextures.MC_BUTTON_HOVERED : GuiTextures.MC_BUTTON_DISABLED)
                .onMousePressed(mouseButton -> {
                    if (!enabled) {
                        return false;
                    }

                    PacketFactionMemberManagerAction packet = new PacketFactionMemberManagerAction();
                    packet.action = action;
                    packet.target = target;
                    packet.page = page;
                    WarForgeMod.NETWORK.sendToServer(packet);
                    return true;
                });
    }

    private static ButtonWidget<?> tabButton(String label, int x, int y, boolean selected, FactionMemberManagerGuiData.Page page) {
        return new ButtonWidget<>()
                .width(74)
                .height(18)
                .overlay(IKey.str(label).color(selected ? 0xFFFFFF : 0xCCCCCC))
                .background(selected ? GuiTextures.MC_BUTTON_HOVERED : GuiTextures.MC_BUTTON)
                .onMousePressed(mouseButton -> {
                    if (!selected) {
                        FactionMemberManagerGuiFactory.INSTANCE.openClient(page);
                    }
                    return true;
                })
                .pos(x, y);
    }

    private static String formatRole(Faction.Role role) {
        return switch (role) {
            case LEADER -> "Leader";
            case OFFICER -> "Officer";
            default -> "Member";
        };
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
