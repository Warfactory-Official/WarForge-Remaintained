package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiFactory;
import com.flansmod.warforge.server.Faction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketFactionMemberManagerAction extends PacketBase {
    public enum Action {
        PROMOTE,
        DEMOTE,
        KICK_OR_LEAVE,
        TRANSFER_LEADER,
        INVITE,
        ACCEPT_INVITE
    }

    public Action action = Action.INVITE;
    public UUID target = Faction.nullUuid;
    public FactionMemberManagerGuiData.Page page = FactionMemberManagerGuiData.Page.MEMBERS;

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeByte(action.ordinal());
        writeUUID(data, target);
        data.writeByte(page.ordinal());
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        action = Action.values()[data.readByte()];
        target = readUUID(data);
        page = FactionMemberManagerGuiData.Page.values()[data.readByte()];
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        switch (action) {
            case PROMOTE -> {
                EntityPlayerMP promoteTarget = WarForgeMod.MC_SERVER.getPlayerList().getPlayerByUUID(target);
                if (promoteTarget == null) {
                    playerEntity.sendMessage(new net.minecraft.util.text.TextComponentString("That player must be online to be promoted"));
                } else {
                    WarForgeMod.FACTIONS.requestPromote(playerEntity, promoteTarget);
                }
            }
            case DEMOTE -> {
                EntityPlayerMP demoteTarget = WarForgeMod.MC_SERVER.getPlayerList().getPlayerByUUID(target);
                if (demoteTarget == null) {
                    playerEntity.sendMessage(new net.minecraft.util.text.TextComponentString("That player must be online to be demoted"));
                } else {
                    WarForgeMod.FACTIONS.requestDemote(playerEntity, demoteTarget);
                }
            }
            case KICK_OR_LEAVE -> {
                Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(playerEntity.getUniqueID());
                if (faction != null) {
                    WarForgeMod.FACTIONS.requestRemovePlayerFromFaction(playerEntity, faction.uuid, target);
                }
            }
            case TRANSFER_LEADER -> {
                Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(playerEntity.getUniqueID());
                if (faction != null) {
                    WarForgeMod.FACTIONS.RequestTransferLeadership(playerEntity, faction.uuid, target);
                }
            }
            case INVITE -> WarForgeMod.FACTIONS.requestInvitePlayerToMyFaction(playerEntity, target);
            case ACCEPT_INVITE -> WarForgeMod.FACTIONS.RequestAcceptInvite(playerEntity, target);
        }

        FactionMemberManagerGuiFactory.INSTANCE.open(playerEntity, page);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
