package com.flansmod.warforge.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PacketFlagChunk extends PacketBase {
    public String flagId = "";
    public int width;
    public int height;
    public int partIndex;
    public int totalParts;
    public byte[] data = new byte[0];

    /** Flag images are shipped in chunks well under the vanilla custom-payload size limit. */
    private static final int MAX_CHUNK_BYTES = 1 << 20;

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
        flagId = readUTF(in, PacketSecurity.MAX_FLAG_ID);
        width = in.readShort();
        height = in.readShort();
        partIndex = in.readShort();
        totalParts = in.readShort();
        // never size from the length field alone: cap by chunk limit and remaining buffer
        int length = Math.min(PacketSecurity.clampCount(in.readInt(), MAX_CHUNK_BYTES), in.readableBytes());
        data = new byte[length];
        in.readBytes(data);
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
    }
}
