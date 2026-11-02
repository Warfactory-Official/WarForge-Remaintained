package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.CitadelBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketCreateFaction extends ServerboundPacket
{
    public DimBlockPos citadelPos;
    public String factionName = "";
    public int colour = 0xffffff;

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        writeUTF(data, Keys.id(citadelPos.dim).toString());
        data.writeInt(citadelPos.getX());
        data.writeInt(citadelPos.getY());
        data.writeInt(citadelPos.getZ());
        data.writeInt(colour);
        writeUTF(data, factionName, PacketSecurity.MAX_NAME);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        citadelPos = new DimBlockPos(dim, x, y, z);
        colour = data.readInt();
        factionName = readUTF(data, PacketSecurity.MAX_NAME);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity)
    {
        // GUI opens only at the placer's own citadel; require the same instead of a client-picked position
        if(!PacketSecurity.requireReach(playerEntity, citadelPos, "create faction"))
            return;

        CitadelBlockEntity citadel = PacketSecurity.blockEntityAt(playerEntity, citadelPos, CitadelBlockEntity.class, "create faction");
        if(citadel == null)
            return;

        WarForgeMod.FACTIONS.lifecycle.requestCreate(citadel, playerEntity, factionName, colour & 0xffffff);
    }

    @Override
    public void handleClientSide(Player clientPlayer)
    {
        WarForgeMod.LOGGER.error("create faction message received on client");
    }
}
