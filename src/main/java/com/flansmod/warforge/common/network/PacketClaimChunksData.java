package com.flansmod.warforge.common.network;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.flansmod.warforge.client.ClientClaimChunkCache;
import com.flansmod.warforge.client.ClientTickHandler;
import com.flansmod.warforge.client.GuiClaimManager;
import com.flansmod.warforge.server.Faction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketClaimChunksData extends PacketBase {
    public int dim;
    public int centerX;
    public int centerZ;
    public int radius;
    public boolean openUi;
    public UUID playerFactionId = Faction.nullUuid;
    public int forceLoadedCount;
    public int forceLoadedMax;
    public int claimCount;
    public int claimMax;
    public List<ClaimChunkInfo> chunks = new ArrayList<ClaimChunkInfo>();

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(dim);
        data.writeInt(centerX);
        data.writeInt(centerZ);
        data.writeByte(radius);
        data.writeBoolean(openUi);
        writeUUID(data, playerFactionId);
        data.writeShort(forceLoadedCount);
        data.writeShort(forceLoadedMax);
        data.writeShort(claimCount);
        data.writeShort(claimMax);

        data.writeShort(chunks.size());
        for (ClaimChunkInfo chunk : chunks) {
            data.writeInt(chunk.x);
            data.writeInt(chunk.z);
            writeUUID(data, chunk.factionId);
            writeUTF(data, chunk.factionName);
            data.writeInt(chunk.colour);
            data.writeByte(chunk.flags);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        dim = data.readInt();
        centerX = data.readInt();
        centerZ = data.readInt();
        radius = data.readByte();
        openUi = data.readBoolean();
        playerFactionId = readUUID(data);
        forceLoadedCount = data.readShort();
        forceLoadedMax = data.readShort();
        claimCount = data.readShort();
        claimMax = data.readShort();

        chunks.clear();
        int size = data.readShort();
        for (int i = 0; i < size; i++) {
            ClaimChunkInfo info = new ClaimChunkInfo();
            info.x = data.readInt();
            info.z = data.readInt();
            info.factionId = readUUID(data);
            info.factionName = readUTF(data);
            info.colour = data.readInt();
            info.flags = data.readByte();
            chunks.add(info);
        }
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        // noop
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        ClientClaimChunkCache.replaceAll(dim, centerX, centerZ, radius, playerFactionId, forceLoadedCount, forceLoadedMax, claimCount, claimMax, chunks);
        ClientTickHandler.CLAIMS_DIRTY = true;
        if (openUi) {
            ClientGUI.open(GuiClaimManager.makeGUI(this));
        }
    }
}
