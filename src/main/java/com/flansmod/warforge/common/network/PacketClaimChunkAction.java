package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketClaimChunkAction extends PacketBase {
    public static final byte ACTION_CLAIM = 0;
    public static final byte ACTION_UNCLAIM = 1;
    public static final byte ACTION_TOGGLE_FORCELOAD = 2;

    public DimChunkPos chunk = new DimChunkPos(0, 0, 0);
    public DimChunkPos center = new DimChunkPos(0, 0, 0);
    public int radius = 4;
    public byte action = ACTION_CLAIM;

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(chunk.dim);
        data.writeInt(chunk.x);
        data.writeInt(chunk.z);
        data.writeByte(action);
        data.writeInt(center.dim);
        data.writeInt(center.x);
        data.writeInt(center.z);
        data.writeByte(radius);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        chunk = new DimChunkPos(data.readInt(), data.readInt(), data.readInt());
        action = data.readByte();
        center = new DimChunkPos(data.readInt(), data.readInt(), data.readInt());
        radius = data.readByte();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        switch (action) {
            case ACTION_CLAIM:
                WarForgeMod.FACTIONS.requestClaimChunkNoTile(playerEntity, chunk);
                break;
            case ACTION_UNCLAIM:
                WarForgeMod.FACTIONS.requestRemoveClaimByChunk(playerEntity, chunk);
                break;
            case ACTION_TOGGLE_FORCELOAD:
                WarForgeMod.FACTIONS.requestToggleForceLoad(playerEntity, chunk);
                break;
            default:
                break;
        }

        int clampedRadius = Math.max(1, Math.min(radius, WarForgeConfig.CLAIM_MANAGER_RADIUS));
        WarForgeMod.FACTIONS.sendClaimChunks(playerEntity, center, clampedRadius, true);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        // noop
    }
}
