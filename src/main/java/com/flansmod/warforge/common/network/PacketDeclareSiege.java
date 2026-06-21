package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// Client -> server. Declares a camp-less siege chosen from the claim map: stage 1 picks the target
// chunk, stage 2 picks the chunk the siege is launched from. The server re-validates everything
// (range, ownership, sieg-ability, cooldown, item cost) in FactionStorage#requestDeclareSiege, so a
// crafted packet can do nothing the UI couldn't legitimately do.
public class PacketDeclareSiege extends PacketBase {
    public DimChunkPos targetChunk = new DimChunkPos(Level.OVERWORLD, 0, 0);
    public DimChunkPos fromChunk = new DimChunkPos(Level.OVERWORLD, 0, 0);

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeUtf(targetChunk.dim.location().toString());
        data.writeInt(targetChunk.x);
        data.writeInt(targetChunk.z);
        data.writeUtf(fromChunk.dim.location().toString());
        data.writeInt(fromChunk.x);
        data.writeInt(fromChunk.z);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        ResourceKey<Level> targetDim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.readUtf()));
        targetChunk = new DimChunkPos(targetDim, data.readInt(), data.readInt());
        ResourceKey<Level> fromDim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.readUtf()));
        fromChunk = new DimChunkPos(fromDim, data.readInt(), data.readInt());
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        WarForgeMod.FACTIONS.requestDeclareSiege(playerEntity, targetChunk, fromChunk);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Received declare siege packet client side");
    }
}
