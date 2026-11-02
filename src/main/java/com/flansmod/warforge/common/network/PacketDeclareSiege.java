package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

// server re-validates everything in FactionStorage#requestDeclareSiege
public class PacketDeclareSiege extends ServerboundPacket {
    public DimChunkPos targetChunk = new DimChunkPos(Level.OVERWORLD, 0, 0);
    public DimChunkPos fromChunk = new DimChunkPos(Level.OVERWORLD, 0, 0);

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeUtf(Keys.id(targetChunk.dim).toString());
        data.writeInt(targetChunk.x);
        data.writeInt(targetChunk.z);
        data.writeUtf(Keys.id(fromChunk.dim).toString());
        data.writeInt(fromChunk.x);
        data.writeInt(fromChunk.z);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        ResourceKey<Level> targetDim = PacketSecurity.readDimOrOverworld(data);
        targetChunk = new DimChunkPos(targetDim, data.readInt(), data.readInt());
        ResourceKey<Level> fromDim = PacketSecurity.readDimOrOverworld(data);
        fromChunk = new DimChunkPos(fromDim, data.readInt(), data.readInt());
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        if (!PacketSecurity.requireSameDim(playerEntity, fromChunk.dim, "declare siege")) {
            return;
        }
        WarForgeMod.FACTIONS.siegeService.requestDeclareSiege(playerEntity, targetChunk, fromChunk);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Received declare siege packet client side");
    }
}
