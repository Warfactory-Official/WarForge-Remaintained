package com.flansmod.warforge.common.factories;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.GuiFactionMemberManager;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.common.WarForgeMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public class FactionMemberManagerGuiFactory extends AbstractUIFactory<FactionMemberManagerGuiData> {
    public static final FactionMemberManagerGuiFactory INSTANCE = new FactionMemberManagerGuiFactory();

    private static final IGuiHolder<FactionMemberManagerGuiData> HOLDER = new IGuiHolder<FactionMemberManagerGuiData>() {
        @Override
        public ModularPanel buildUI(FactionMemberManagerGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
            if (guiData.isClient()) {
                return GuiFactionMemberManager.buildPanel(guiData);
            }
            return ModularPanel.defaultPanel("faction_member_manager")
                    .width(360)
                    .height(260)
                    .topRel(0.40f);
        }

        @Override
        public ModularScreen createScreen(FactionMemberManagerGuiData guiData, ModularPanel mainPanel) {
            return new ModularScreen(Tags.MODID, mainPanel);
        }
    };

    private FactionMemberManagerGuiFactory() {
        super("warforge:faction_members");
    }

    public static void init() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    public void open(EntityPlayer player, FactionMemberManagerGuiData.Page page) {
        EntityPlayerMP serverPlayer = verifyServerSide(player);
        GuiManager.open(this, createServerData(serverPlayer, page), serverPlayer);
    }

    @SideOnly(Side.CLIENT)
    public void openClient(FactionMemberManagerGuiData.Page page) {
        GuiManager.openFromClient(this, new FactionMemberManagerGuiData(verifyClientSide(Platform.getClientPlayer()), page));
    }

    @Override
    public @NotNull IGuiHolder<FactionMemberManagerGuiData> getGuiHolder(FactionMemberManagerGuiData guiData) {
        return HOLDER;
    }

    @Override
    public void writeGuiData(FactionMemberManagerGuiData guiData, PacketBuffer packetBuffer) {
        if (guiData.isClient()) {
            packetBuffer.writeByte(guiData.page.ordinal());
            return;
        }

        packetBuffer.writeByte(guiData.page.ordinal());
        packetBuffer.writeBoolean(guiData.hasFaction);
        packetBuffer.writeLong(guiData.factionId.getMostSignificantBits());
        packetBuffer.writeLong(guiData.factionId.getLeastSignificantBits());
        packetBuffer.writeString(guiData.factionName);
        packetBuffer.writeInt(guiData.factionColor);
        packetBuffer.writeByte(guiData.viewerRole.ordinal());
        packetBuffer.writeBoolean(guiData.canManageMembers);
        packetBuffer.writeBoolean(guiData.canInvitePlayers);

        packetBuffer.writeShort(guiData.members.size());
        for (FactionMemberManagerGuiData.MemberEntry member : guiData.members) {
            packetBuffer.writeLong(member.playerId.getMostSignificantBits());
            packetBuffer.writeLong(member.playerId.getLeastSignificantBits());
            packetBuffer.writeString(member.username);
            packetBuffer.writeByte(member.role.ordinal());
            packetBuffer.writeBoolean(member.online);
            packetBuffer.writeBoolean(member.self);
            packetBuffer.writeBoolean(member.canKickOrLeave);
            packetBuffer.writeBoolean(member.canPromote);
            packetBuffer.writeBoolean(member.canDemote);
            packetBuffer.writeBoolean(member.canTransferLeadership);
        }

        packetBuffer.writeShort(guiData.inviteCandidates.size());
        for (FactionMemberManagerGuiData.InviteEntry invite : guiData.inviteCandidates) {
            packetBuffer.writeLong(invite.playerId.getMostSignificantBits());
            packetBuffer.writeLong(invite.playerId.getLeastSignificantBits());
            packetBuffer.writeString(invite.username);
            packetBuffer.writeLong(invite.factionId.getMostSignificantBits());
            packetBuffer.writeLong(invite.factionId.getLeastSignificantBits());
            packetBuffer.writeInt(invite.factionColor);
            packetBuffer.writeLong(invite.inviterId.getMostSignificantBits());
            packetBuffer.writeLong(invite.inviterId.getLeastSignificantBits());
            packetBuffer.writeString(invite.inviterName);
            packetBuffer.writeBoolean(invite.incoming);
            packetBuffer.writeBoolean(invite.invited);
            packetBuffer.writeBoolean(invite.canInvite);
            packetBuffer.writeBoolean(invite.canAccept);
        }
    }

    @Override
    public @NotNull FactionMemberManagerGuiData readGuiData(EntityPlayer entityPlayer, PacketBuffer packetBuffer) {
        FactionMemberManagerGuiData.Page page = FactionMemberManagerGuiData.Page.values()[packetBuffer.readByte()];
        if (!entityPlayer.world.isRemote) {
            return createServerData((EntityPlayerMP) entityPlayer, page);
        }

        FactionMemberManagerGuiData data = new FactionMemberManagerGuiData(entityPlayer, page);
        data.hasFaction = packetBuffer.readBoolean();
        data.factionId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
        data.factionName = packetBuffer.readString(32767);
        data.factionColor = packetBuffer.readInt();
        data.viewerRole = Faction.Role.values()[packetBuffer.readByte()];
        data.canManageMembers = packetBuffer.readBoolean();
        data.canInvitePlayers = packetBuffer.readBoolean();

        int memberCount = packetBuffer.readShort();
        for (int i = 0; i < memberCount; i++) {
            FactionMemberManagerGuiData.MemberEntry member = new FactionMemberManagerGuiData.MemberEntry();
            member.playerId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
            member.username = packetBuffer.readString(32767);
            member.role = Faction.Role.values()[packetBuffer.readByte()];
            member.online = packetBuffer.readBoolean();
            member.self = packetBuffer.readBoolean();
            member.canKickOrLeave = packetBuffer.readBoolean();
            member.canPromote = packetBuffer.readBoolean();
            member.canDemote = packetBuffer.readBoolean();
            member.canTransferLeadership = packetBuffer.readBoolean();
            data.members.add(member);
        }

        int inviteCount = packetBuffer.readShort();
        for (int i = 0; i < inviteCount; i++) {
            FactionMemberManagerGuiData.InviteEntry invite = new FactionMemberManagerGuiData.InviteEntry();
            invite.playerId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
            invite.username = packetBuffer.readString(32767);
            invite.factionId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
            invite.factionColor = packetBuffer.readInt();
            invite.inviterId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
            invite.inviterName = packetBuffer.readString(32767);
            invite.incoming = packetBuffer.readBoolean();
            invite.invited = packetBuffer.readBoolean();
            invite.canInvite = packetBuffer.readBoolean();
            invite.canAccept = packetBuffer.readBoolean();
            data.inviteCandidates.add(invite);
        }
        return data;
    }

    private FactionMemberManagerGuiData createServerData(EntityPlayerMP player, FactionMemberManagerGuiData.Page page) {
        FactionMemberManagerGuiData data = new FactionMemberManagerGuiData(player, page);
        Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUniqueID());
        if (faction == null) {
            if (page == FactionMemberManagerGuiData.Page.INVITES) {
                for (Faction inviteFaction : WarForgeMod.FACTIONS.getFactionsWithOpenInvitesTo(player.getUniqueID())) {
                    FactionMemberManagerGuiData.InviteEntry invite = new FactionMemberManagerGuiData.InviteEntry();
                    invite.incoming = true;
                    invite.factionId = inviteFaction.uuid;
                    invite.factionColor = inviteFaction.colour;
                    invite.username = inviteFaction.name;
                    invite.playerId = inviteFaction.getLeaderId();
                    invite.inviterId = invite.playerId;
                    invite.inviterName = invite.playerId.equals(Faction.nullUuid) ? "" : resolveName(invite.playerId);
                    invite.invited = true;
                    invite.canAccept = true;
                    data.inviteCandidates.add(invite);
                }
                data.inviteCandidates.sort(Comparator.comparing(invite -> invite.username, String.CASE_INSENSITIVE_ORDER));
            }
            return data;
        }

        data.hasFaction = true;
        data.factionId = faction.uuid;
        data.factionName = faction.name;
        data.factionColor = faction.colour;
        data.viewerRole = determineViewerRole(faction, player.getUniqueID());
        data.canManageMembers = WarForgeMod.isOp(player) || data.viewerRole.ordinal() >= Faction.Role.OFFICER.ordinal();
        data.canInvitePlayers = data.canManageMembers;

        for (Map.Entry<UUID, Faction.PlayerData> entry : faction.members.entrySet()) {
            UUID memberId = entry.getKey();
            FactionMemberManagerGuiData.MemberEntry member = new FactionMemberManagerGuiData.MemberEntry();
            member.playerId = memberId;
            member.username = resolveName(memberId);
            member.role = entry.getValue().role;
            member.online = WarForgeMod.MC_SERVER.getPlayerList().getPlayerByUUID(memberId) != null;
            member.self = player.getUniqueID().equals(memberId);
            member.canKickOrLeave = member.self || WarForgeMod.isOp(player) || faction.isPlayerOutrankingOfficer(player.getUniqueID(), memberId);
            member.canPromote = data.viewerRole == Faction.Role.LEADER && member.role == Faction.Role.MEMBER && member.online;
            member.canDemote = data.viewerRole == Faction.Role.LEADER && member.role == Faction.Role.OFFICER && member.online;
            member.canTransferLeadership = (WarForgeMod.isOp(player) || data.viewerRole == Faction.Role.LEADER) && !member.self;
            data.members.add(member);
        }

        data.members.sort(Comparator
                .comparingInt((FactionMemberManagerGuiData.MemberEntry member) -> -member.role.ordinal())
                .thenComparing(member -> member.username, String.CASE_INSENSITIVE_ORDER));

        for (EntityPlayerMP onlinePlayer : WarForgeMod.MC_SERVER.getPlayerList().getPlayers()) {
            if (onlinePlayer.getUniqueID().equals(player.getUniqueID())) {
                continue;
            }
            if (WarForgeMod.FACTIONS.getFactionOfPlayer(onlinePlayer.getUniqueID()) != null) {
                continue;
            }

            FactionMemberManagerGuiData.InviteEntry invite = new FactionMemberManagerGuiData.InviteEntry();
            invite.playerId = onlinePlayer.getUniqueID();
            invite.username = onlinePlayer.getName();
            invite.invited = faction.isInvitingPlayer(invite.playerId);
            invite.canInvite = data.canInvitePlayers && !invite.invited;
            data.inviteCandidates.add(invite);
        }

        data.inviteCandidates.sort(Comparator.comparing(invite -> invite.username, String.CASE_INSENSITIVE_ORDER));
        return data;
    }

    private static Faction.Role determineViewerRole(Faction faction, UUID playerId) {
        if (faction.isPlayerRoleInFaction(playerId, Faction.Role.LEADER)) {
            return Faction.Role.LEADER;
        }
        if (faction.isPlayerRoleInFaction(playerId, Faction.Role.OFFICER)) {
            return Faction.Role.OFFICER;
        }
        return Faction.Role.MEMBER;
    }

    private static String resolveName(UUID playerId) {
        GameProfile profile = WarForgeMod.MC_SERVER.getPlayerProfileCache().getProfileByUUID(playerId);
        return profile == null ? "Unknown Player" : profile.getName();
    }
}
