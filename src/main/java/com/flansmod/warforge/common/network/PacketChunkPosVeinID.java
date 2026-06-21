package com.flansmod.warforge.common.network;

import org.apache.commons.lang3.tuple.Pair;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.init.VeinUtils;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Siege;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static com.flansmod.warforge.client.ClientProxy.CHUNK_VEIN_CACHE;
import static com.flansmod.warforge.common.WarForgeMod.VEIN_HANDLER;

public class PacketChunkPosVeinID extends PacketBase {
    public DimChunkPos veinLocation = null;
    public short resultInfo = VeinUtils.NULL_VEIN_ID;

    // clients ask for data, servers send data
    // called by the packet handler to convert to a byte stream to send
    @Override
    public void encodeInto(FriendlyByteBuf data) {
        if (veinLocation == null) {
            WarForgeMod.LOGGER.atError().log("Premature ChunkPosVeinID packet with null vein location.");
            return;  // don't encode anything
        }

        // encode the chunk position and resultInfo data
        data.writeUtf(veinLocation.dim.location().toString());
        data.writeInt(veinLocation.x);
        data.writeInt(veinLocation.z);
        data.writeShort(resultInfo);
    }

    // called by the packet handler to make the packet from a byte stream after construction
    @Override
    public void decodeInto(FriendlyByteBuf data) {
        try {
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.readUtf()));
            veinLocation = new DimChunkPos(dim, data.readInt(), data.readInt());
            resultInfo = data.readShort();
        } catch (Exception exception) {
            WarForgeMod.LOGGER.atError().log("Received a faulty ChunkVeinPosPacket: " + data.toString());
            veinLocation = null;
            resultInfo = VeinUtils.NULL_VEIN_ID;
        }
    }

    // always called on packet after decodeInto has been called
    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        if (veinLocation == null) {
            WarForgeMod.LOGGER.atError().log("Decoded ChunkPosVeinID packet without position; ignoring packet.");
            return;
        }

        // check if the player should even receive data about this chunk
        DimChunkPos playerPos = new DimChunkPos(playerEntity.level().dimension(), playerEntity.blockPosition());
        if (!Siege.isPlayerInRadius(veinLocation, playerPos)) {
            WarForgeMod.LOGGER.atError().log("Detected player outside <" + playerPos.toString() +
                    "> of queried chunk's <" + veinLocation.toString() + "> radius. Dropping packet");
            return;
        }

        // if the player is within a reasonable sqr radius (1) of the queried chunk, process and send data
        long seed = ((ServerLevel) playerEntity.level()).getSeed();
        Pair<Vein, Quality> veinInfo = VEIN_HANDLER.getVein(veinLocation.dim, veinLocation.x, veinLocation.z, seed);
        resultInfo = VEIN_HANDLER.compressVeinInfo(veinInfo);
        WarForgeMod.NETWORK.sendTo(this, playerEntity);  // 'encode into' handles getting of important data
    }

    // always called on packet after decodeInto has been called
    @Override
    public void handleClientSide(Player clientPlayer) {
        if (veinLocation == null) { return; }
        CHUNK_VEIN_CACHE.add(veinLocation, resultInfo);  // we now have the necessary data about the vein
    }
}
