package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PacketFactionMemberManagerAction extends ServerboundPacket {
    public enum Action {
        PROMOTE,
        DEMOTE,
        KICK_OR_LEAVE,
        TRANSFER_LEADER,
        INVITE,
        ACCEPT_INVITE
    }

    public Action action = Action.INVITE;
    public UUID target = FactionIds.NONE;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeByte(action.ordinal());
        writeUUID(data, target);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        action = PacketSecurity.readEnum(data, Action.values(), Action.INVITE);
        target = readUUID(data);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        switch (action) {
            case PROMOTE -> {
                ServerPlayer promoteTarget = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(target);
                if (promoteTarget == null) {
                    playerEntity.sendSystemMessage(Component.translatable("warforge.common.promote_offline"));
                } else {
                    WarForgeMod.FACTIONS.membership.requestPromote(playerEntity, promoteTarget);
                }
            }
            case DEMOTE -> {
                ServerPlayer demoteTarget = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(target);
                if (demoteTarget == null) {
                    playerEntity.sendSystemMessage(Component.translatable("warforge.common.demote_offline"));
                } else {
                    WarForgeMod.FACTIONS.membership.requestDemote(playerEntity, demoteTarget);
                }
            }
            case KICK_OR_LEAVE -> {
                Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(playerEntity.getUUID());
                if (faction != null) {
                    WarForgeMod.FACTIONS.membership.requestRemovePlayer(playerEntity.createCommandSourceStack(), faction.getUuid(), target);
                }
            }
            case TRANSFER_LEADER -> {
                Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(playerEntity.getUUID());
                if (faction != null) {
                    WarForgeMod.FACTIONS.membership.requestTransferLeadership(playerEntity, faction.getUuid(), target);
                }
            }
            case INVITE -> WarForgeMod.FACTIONS.membership.requestInvitePlayerToMyFaction(playerEntity, target);
            case ACCEPT_INVITE -> WarForgeMod.FACTIONS.membership.acceptInvite(playerEntity, target);
        }
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
    }
}
