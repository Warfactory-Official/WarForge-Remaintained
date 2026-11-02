package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;

import com.flansmod.warforge.compat.Profiles;

import com.flansmod.warforge.compat.PlayerRef;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionMemberManagerGuiData;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.UUID;

public class PacketRequestMemberData extends ServerboundPacket {
    public FactionMemberManagerGuiData.Page page = FactionMemberManagerGuiData.Page.MEMBERS;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeByte(page.ordinal());
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        page = PacketSecurity.readEnum(data, FactionMemberManagerGuiData.Page.values(), FactionMemberManagerGuiData.Page.MEMBERS);
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        PacketMemberData response = buildResponse(player, page);
        WarForgeMod.NETWORK.sendTo(response, player);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Received PacketRequestMemberData on client side");
    }

    private static PacketMemberData buildResponse(ServerPlayer player, FactionMemberManagerGuiData.Page page) {
        PacketMemberData data = new PacketMemberData();
        data.page = page;

        Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            if (page == FactionMemberManagerGuiData.Page.INVITES) {
                for (Faction inviteFaction : WarForgeMod.FACTIONS.membership.getFactionsWithOpenInvitesTo(player.getUUID())) {
                    FactionMemberManagerGuiData.InviteEntry invite = new FactionMemberManagerGuiData.InviteEntry();
                    invite.incoming = true;
                    invite.factionId = inviteFaction.getUuid();
                    invite.factionColor = inviteFaction.getColour();
                    invite.username = inviteFaction.getName();
                    invite.playerId = inviteFaction.getLeaderId();
                    invite.inviterId = invite.playerId;
                    invite.inviterName = invite.playerId.equals(FactionIds.NONE) ? "" : resolveName(invite.playerId);
                    invite.invited = true;
                    invite.canAccept = true;
                    data.inviteCandidates.add(invite);
                }
                data.inviteCandidates.sort(Comparator.comparing(invite -> invite.username, String.CASE_INSENSITIVE_ORDER));
            }
            return data;
        }

        data.hasFaction = true;
        data.factionId = faction.getUuid();
        data.factionName = faction.getName();
        data.factionColor = faction.getColour();
        data.viewerRole = determineViewerRole(faction, player.getUUID());
        data.canManageMembers = WarForgeMod.isOp(player) || data.viewerRole.ordinal() >= Role.OFFICER.ordinal();
        data.canInvitePlayers = data.canManageMembers;
        data.canManageAlliances = data.canManageMembers;
        data.allowAllyInteraction = faction.isAllowAllyInteraction();

        for (UUID memberId : faction.getMemberIds()) {
            FactionMemberManagerGuiData.MemberEntry member = new FactionMemberManagerGuiData.MemberEntry();
            member.playerId = memberId;
            member.username = resolveName(memberId);
            member.role = faction.getRole(memberId);
            member.online = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(memberId) != null;
            member.self = player.getUUID().equals(memberId);
            member.canKickOrLeave = member.self || WarForgeMod.isOp(player) || faction.isPlayerOutrankingOfficer(player.getUUID(), memberId);
            member.canPromote = data.viewerRole == Role.LEADER && member.role == Role.MEMBER && member.online;
            member.canDemote = data.viewerRole == Role.LEADER && member.role == Role.OFFICER && member.online;
            member.canTransferLeadership = (WarForgeMod.isOp(player) || data.viewerRole == Role.LEADER) && !member.self;
            data.members.add(member);
        }

        data.members.sort(Comparator
                .comparingInt((FactionMemberManagerGuiData.MemberEntry member) -> -member.role.ordinal())
                .thenComparing(member -> member.username, String.CASE_INSENSITIVE_ORDER));

        for (ServerPlayer onlinePlayer : WarForgeMod.MC_SERVER.getPlayerList().getPlayers()) {
            if (onlinePlayer.getUUID().equals(player.getUUID())) {
                continue;
            }
            if (WarForgeMod.FACTIONS.registry.getFactionOfPlayer(onlinePlayer.getUUID()) != null) {
                continue;
            }
            FactionMemberManagerGuiData.InviteEntry invite = new FactionMemberManagerGuiData.InviteEntry();
            invite.playerId = onlinePlayer.getUUID();
            invite.username = onlinePlayer.getName().getString();
            invite.invited = faction.isInvitingPlayer(invite.playerId);
            invite.canInvite = data.canInvitePlayers && !invite.invited;
            data.inviteCandidates.add(invite);
        }

        data.inviteCandidates.sort(Comparator.comparing(invite -> invite.username, String.CASE_INSENSITIVE_ORDER));

        if (page == FactionMemberManagerGuiData.Page.ALLIANCES) {
            for (UUID allyId : faction.getAllies()) {
                addAllianceEntry(data, WarForgeMod.FACTIONS.registry.getFaction(allyId), FactionMemberManagerGuiData.AllianceEntry.KIND_ALLY);
            }
            for (UUID requesterId : faction.getPendingAllianceRequests()) {
                addAllianceEntry(data, WarForgeMod.FACTIONS.registry.getFaction(requesterId), FactionMemberManagerGuiData.AllianceEntry.KIND_PENDING);
            }
            for (Faction invitable : WarForgeMod.FACTIONS.alliances.getAlliableFactions(faction)) {
                if (faction.getPendingAllianceRequests().contains(invitable.getUuid())) {
                    continue;
                }
                addAllianceEntry(data, invitable, FactionMemberManagerGuiData.AllianceEntry.KIND_INVITABLE);
            }
        }

        return data;
    }

    private static void addAllianceEntry(PacketMemberData data, Faction faction, byte kind) {
        if (faction == null) {
            return;
        }
        FactionMemberManagerGuiData.AllianceEntry entry = new FactionMemberManagerGuiData.AllianceEntry();
        entry.factionId = faction.getUuid();
        entry.factionName = faction.getName();
        entry.factionColor = faction.getColour();
        entry.onlineCount = faction.countOnlineMembers(null);
        entry.kind = kind;
        data.alliances.add(entry);
    }

    private static Role determineViewerRole(Faction faction, UUID playerId) {
        if (faction.isPlayerRoleInFaction(playerId, Role.LEADER)) {
            return Role.LEADER;
        }
        if (faction.isPlayerRoleInFaction(playerId, Role.OFFICER)) {
            return Role.OFFICER;
        }
        return Role.MEMBER;
    }

    private static String resolveName(UUID playerId) {
        PlayerRef profile = Profiles.get(WarForgeMod.MC_SERVER, playerId).orElse(null);
        return profile == null ? "Unknown Player" : profile.getName();
    }
}
