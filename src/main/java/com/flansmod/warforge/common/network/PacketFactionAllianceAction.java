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

// Client -> server alliance management, driven from the Alliances tab of the faction members GUI.
// target is the other faction's UUID (ignored for TOGGLE_ALLY_BUILD). The server re-validates rank
// and state in FactionStorage, then re-opens the GUI so the client sees the updated data.
public class PacketFactionAllianceAction extends PacketBase {
    public enum Action {
        INVITE,
        ACCEPT,
        DECLINE,
        BREAK,
        TOGGLE_ALLY_BUILD
    }

    public Action action = Action.INVITE;
    public UUID target = Faction.nullUuid;
    public FactionMemberManagerGuiData.Page page = FactionMemberManagerGuiData.Page.ALLIANCES;

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
            case INVITE -> WarForgeMod.FACTIONS.requestInviteAlly(playerEntity, target);
            case ACCEPT -> WarForgeMod.FACTIONS.requestAcceptAlliance(playerEntity, target);
            case DECLINE -> WarForgeMod.FACTIONS.requestDeclineAlliance(playerEntity, target);
            case BREAK -> WarForgeMod.FACTIONS.requestBreakAlliance(playerEntity, target);
            case TOGGLE_ALLY_BUILD -> WarForgeMod.FACTIONS.requestToggleAllyInteraction(playerEntity);
        }
        FactionMemberManagerGuiFactory.INSTANCE.open(playerEntity, page);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
