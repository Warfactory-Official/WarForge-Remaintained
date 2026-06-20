package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

// Client -> server. Asks the server to sample and ship terrain colours for a chunk region so the
// claim map can render real terrain for chunks the client has not loaded (e.g. a distant siege
// target picked in the declaration UI). The server replies with a PacketTerrainColors.
public class PacketRequestTerrainColors extends PacketBase {
    public DimChunkPos center = new DimChunkPos(0, 0, 0);
    public int radius = 4;

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(center.dim);
        data.writeInt(center.x);
        data.writeInt(center.z);
        data.writeByte(radius);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        center = new DimChunkPos(data.readInt(), data.readInt(), data.readInt());
        radius = data.readByte();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        WarForgeMod.FACTIONS.sendTerrainColors(playerEntity, center, radius);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        // noop
    }
}
