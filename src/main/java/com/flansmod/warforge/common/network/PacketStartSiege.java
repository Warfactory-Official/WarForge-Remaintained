package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketStartSiege extends ServerboundPacket {
    public DimBlockPos siegeCampPos;
    public Vec3i offset;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeUtf(Keys.id(siegeCampPos.dim).toString());
        data.writeInt(siegeCampPos.getX());
        data.writeInt(siegeCampPos.getY());
        data.writeInt(siegeCampPos.getZ());

        data.writeByte(offset.getX());
        data.writeByte(offset.getZ());
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        siegeCampPos = new DimBlockPos(dim, x, y, z);
        byte dx = data.readByte();
        byte dz = data.readByte();
        offset = new Vec3i(dx, 0, dz);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        if (!PacketSecurity.requireReach(playerEntity, siegeCampPos, "start siege"))
            return;

        WarForgeMod.FACTIONS.siegeService.requestStartSiege(playerEntity, siegeCampPos, offset);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Received start siege packet client side");
    }
}
