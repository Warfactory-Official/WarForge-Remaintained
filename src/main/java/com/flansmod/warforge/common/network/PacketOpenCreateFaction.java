package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketOpenCreateFaction extends PacketBase {
    public static DimBlockPos pendingCitadelPos;

    public DimBlockPos pos;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUTF(data, Keys.id(pos.dim).toString());
        data.writeInt(pos.getX());
        data.writeInt(pos.getY());
        data.writeInt(pos.getZ());
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        pos = new DimBlockPos(dim, x, y, z);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
    }
}
