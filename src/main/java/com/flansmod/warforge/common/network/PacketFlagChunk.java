package com.flansmod.warforge.common.network;

import com.flansmod.warforge.client.ClientFlagRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketFlagChunk extends PacketBase {
    public String flagId = "";
    public int width;
    public int height;
    public int partIndex;
    public int totalParts;
    public byte[] data = new byte[0];

    @Override
    public void encodeInto(FriendlyByteBuf out) {
        writeUTF(out, flagId);
        out.writeShort(width);
        out.writeShort(height);
        out.writeShort(partIndex);
        out.writeShort(totalParts);
        out.writeInt(data.length);
        out.writeBytes(data);
    }

    @Override
    public void decodeInto(FriendlyByteBuf in) {
        flagId = readUTF(in);
        width = in.readShort();
        height = in.readShort();
        partIndex = in.readShort();
        totalParts = in.readShort();
        int length = in.readInt();
        data = new byte[length];
        in.readBytes(data);
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        ClientFlagRegistry.receiveCustomFlagChunk(flagId, width, height, partIndex, totalParts, data);
    }
}
