package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketRequestClaimChunks extends PacketBase {
    public DimChunkPos center = new DimChunkPos(Level.OVERWORLD, 0, 0);
    public int radius = 4;
    public boolean outlineOnly = false;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeUtf(Keys.id(center.dim).toString());
        data.writeInt(center.x);
        data.writeInt(center.z);
        data.writeByte(radius);
        data.writeBoolean(outlineOnly);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
        center = new DimChunkPos(dim, data.readInt(), data.readInt());
        radius = data.readByte();
        outlineOnly = data.readBoolean();
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        WarForgeMod.FACTIONS.sendClaimChunks(playerEntity, center, radius, outlineOnly);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
    }
}
