package com.flansmod.warforge.common.network;

import com.flansmod.warforge.api.modularui.ChunkMapTextureDaemon;
import com.flansmod.warforge.client.ServerTerrainCache;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

// Server -> client. Per-block vanilla map colours + heights for a chunk region, sampled server-side
// (MapBlockColorSampler is client-only, so the server uses the vanilla MapColor palette). The client
// caches these and the chunk-map texture daemon uses them for chunks it has not loaded, so a distant
// siege target region shows real terrain instead of a flat placeholder.
public class PacketTerrainColors extends PacketBase {
    public int dim;
    public int centerX;
    public int centerZ;
    public int radius;
    public final List<int[]> chunkCoords = new ArrayList<>(); // each entry {chunkX, chunkZ}
    public final List<int[]> colors = new ArrayList<>();       // each entry 256 ints, 0xRRGGBB top block
    public final List<int[]> heights = new ArrayList<>();      // each entry 256 ints, column heightmap value

    public void addChunk(int chunkX, int chunkZ, int[] chunkColors, int[] chunkHeights) {
        chunkCoords.add(new int[]{chunkX, chunkZ});
        colors.add(chunkColors);
        heights.add(chunkHeights);
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(dim);
        data.writeInt(centerX);
        data.writeInt(centerZ);
        data.writeByte(radius);
        data.writeInt(chunkCoords.size());
        for (int i = 0; i < chunkCoords.size(); i++) {
            int[] coord = chunkCoords.get(i);
            data.writeInt(coord[0]);
            data.writeInt(coord[1]);
            int[] chunkColors = colors.get(i);
            int[] chunkHeights = heights.get(i);
            for (int c = 0; c < 256; c++) {
                data.writeMedium(chunkColors[c] & 0x00FFFFFF); // 3 bytes RGB
            }
            for (int h = 0; h < 256; h++) {
                data.writeByte(chunkHeights[h] & 0xFF);
            }
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        dim = data.readInt();
        centerX = data.readInt();
        centerZ = data.readInt();
        radius = data.readByte();
        int count = data.readInt();
        for (int i = 0; i < count; i++) {
            int cx = data.readInt();
            int cz = data.readInt();
            int[] chunkColors = new int[256];
            int[] chunkHeights = new int[256];
            for (int c = 0; c < 256; c++) {
                chunkColors[c] = data.readUnsignedMedium();
            }
            for (int h = 0; h < 256; h++) {
                chunkHeights[h] = data.readUnsignedByte();
            }
            addChunk(cx, cz, chunkColors, chunkHeights);
        }
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        // noop
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer clientPlayer) {
        for (int i = 0; i < chunkCoords.size(); i++) {
            int[] coord = chunkCoords.get(i);
            ServerTerrainCache.put(dim, coord[0], coord[1], colors.get(i), heights.get(i));
        }
        // Terrain arriving doesn't change the map-request key, so force the claim map to rebuild its
        // textures now that the server colours are available.
        ChunkMapTextureDaemon.rebuildLast("claimmap");
    }

    @Override
    public boolean canUseCompression() {
        return true;
    }
}
