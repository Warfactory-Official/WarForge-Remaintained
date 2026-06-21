package com.flansmod.warforge.common.network;

import com.flansmod.warforge.client.ClientFlagRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class PacketFlagManifest extends PacketBase {
    public List<String> flagIds = new ArrayList<String>();

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeShort(flagIds.size());
        for (String flagId : flagIds) {
            writeUTF(data, flagId);
        }
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        flagIds.clear();
        int size = data.readShort();
        for (int i = 0; i < size; i++) {
            flagIds.add(readUTF(data));
        }
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        ClientFlagRegistry.setAvailableFlags(flagIds);
    }
}
