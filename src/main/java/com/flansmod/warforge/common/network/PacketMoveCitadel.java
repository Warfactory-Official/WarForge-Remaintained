package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketMoveCitadel extends ServerboundPacket
{
    public DimBlockPos pos = DimBlockPos.ZERO;

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeUtf(Keys.id(pos.dim).toString());
        data.writeInt(pos.getX());
        data.writeInt(pos.getY());
        data.writeInt(pos.getZ());
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
        pos = new DimBlockPos(dim, data.readInt(), data.readInt(), data.readInt());
    }

    @Override
    public void handleServerSide(ServerPlayer player)
    {
        // empty packet = GUI prompt; a real target is ray-picked within 10 blocks, farther => not from the UI
        if (!pos.equals(DimBlockPos.ZERO) && !PacketSecurity.requireReach(player, pos, "move citadel"))
            return;

        WarForgeMod.FACTIONS.citadels.requestMoveCitadel(player, pos);
    }

    @Override
    public void handleClientSide(Player clientPlayer)
    {
    }
}
