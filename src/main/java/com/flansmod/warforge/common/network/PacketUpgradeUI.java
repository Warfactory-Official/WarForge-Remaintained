package com.flansmod.warforge.common.network;

import brachy.modularui.factory.ClientGUI;
import com.flansmod.warforge.client.GUIUpgradePanel;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

public class PacketUpgradeUI extends PacketBase {

    public UUID mFactionID = Faction.nullUuid;
    public String mFactionName = "";
    int level = 0;
    int color = 0xffff;
    boolean outrankingOfficer = false;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUUID(data, mFactionID);
        writeUTF(data, mFactionName);
        data.writeInt(level);
        data.writeInt(color);
        data.writeBoolean(outrankingOfficer);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        mFactionID = readUUID(data);
        mFactionName = readUTF(data);
        level = data.readInt();
        color = data.readInt();
        outrankingOfficer = data.readBoolean();
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        WarForgeMod.LOGGER.error("Received a Upgrade UI packet on serverside");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleClientSide(Player clientPlayer) {
        ClientGUI.open(GUIUpgradePanel.createGui(
                mFactionID,
                mFactionName,
                level,
                color,
                outrankingOfficer
                ));
    }
}
