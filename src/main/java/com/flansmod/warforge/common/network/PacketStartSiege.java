package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PacketStartSiege extends PacketBase {
    public DimBlockPos mSiegeCampPos;
    public Vec3i mOffset;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeUtf(mSiegeCampPos.dim.location().toString());
        data.writeInt(mSiegeCampPos.getX());
        data.writeInt(mSiegeCampPos.getY());
        data.writeInt(mSiegeCampPos.getZ());

        data.writeByte(mOffset.getX());
        data.writeByte(mOffset.getZ());
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.readUtf()));
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        mSiegeCampPos = new DimBlockPos(dim, x, y, z);
        byte dx = data.readByte();
        byte dz = data.readByte();
        mOffset = new Vec3i(dx, 0, dz);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        WarForgeMod.FACTIONS.requestStartSiege(playerEntity, mSiegeCampPos, mOffset);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Received start siege packet client side");
    }

}
