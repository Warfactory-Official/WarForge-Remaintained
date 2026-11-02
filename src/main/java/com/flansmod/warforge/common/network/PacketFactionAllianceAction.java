package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PacketFactionAllianceAction extends ServerboundPacket {
    public enum Action {
        INVITE,
        ACCEPT,
        DECLINE,
        BREAK,
        TOGGLE_ALLY_BUILD
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
            case INVITE -> WarForgeMod.FACTIONS.alliances.requestInviteAlly(playerEntity, target);
            case ACCEPT -> WarForgeMod.FACTIONS.alliances.requestAcceptAlliance(playerEntity, target);
            case DECLINE -> WarForgeMod.FACTIONS.alliances.requestDeclineAlliance(playerEntity, target);
            case BREAK -> WarForgeMod.FACTIONS.alliances.requestBreakAlliance(playerEntity, target);
            case TOGGLE_ALLY_BUILD -> WarForgeMod.FACTIONS.alliances.requestToggleAllyInteraction(playerEntity);
        }
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
    }
}
