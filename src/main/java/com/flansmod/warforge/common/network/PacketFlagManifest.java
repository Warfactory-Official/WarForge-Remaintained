package com.flansmod.warforge.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

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
        int size = PacketSecurity.clampCount(data.readShort(), 4096);
        for (int i = 0; i < size; i++) {
            flagIds.add(readUTF(data, PacketSecurity.MAX_FLAG_ID));
        }
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
    }
}
