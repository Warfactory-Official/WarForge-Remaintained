package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

// Client -> server. Declares a camp-less siege chosen from the claim map: stage 1 picks the target
// chunk, stage 2 picks the chunk the siege is launched from. The server re-validates everything
// (range, ownership, sieg-ability, cooldown, item cost) in FactionStorage#requestDeclareSiege, so a
// crafted packet can do nothing the UI couldn't legitimately do.
public class PacketDeclareSiege extends PacketBase {
    public DimChunkPos targetChunk = new DimChunkPos(0, 0, 0);
    public DimChunkPos fromChunk = new DimChunkPos(0, 0, 0);

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(targetChunk.dim);
        data.writeInt(targetChunk.x);
        data.writeInt(targetChunk.z);
        data.writeInt(fromChunk.dim);
        data.writeInt(fromChunk.x);
        data.writeInt(fromChunk.z);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targetChunk = new DimChunkPos(data.readInt(), data.readInt(), data.readInt());
        fromChunk = new DimChunkPos(data.readInt(), data.readInt(), data.readInt());
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        WarForgeMod.FACTIONS.requestDeclareSiege(playerEntity, targetChunk, fromChunk);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        WarForgeMod.LOGGER.error("Received declare siege packet client side");
    }
}
