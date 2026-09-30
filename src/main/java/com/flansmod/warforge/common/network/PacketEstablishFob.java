package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityFob;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.flansmod.warforge.compat.Keys;

public class PacketEstablishFob extends PacketBase {
    public DimBlockPos mPos;
    public String mName = "";

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUTF(data, Keys.id(mPos.dim).toString());
        data.writeInt(mPos.getX());
        data.writeInt(mPos.getY());
        data.writeInt(mPos.getZ());
        writeUTF(data, mName);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        mPos = new DimBlockPos(dim, x, y, z);
        mName = readUTF(data, 32);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        // Establishing a FOB is driven from the FOB block's own GUI, so the officer is standing at it.
        if (!PacketSecurity.requireReach(playerEntity, mPos, "establish FOB")) {
            return;
        }

        if (mName == null || mName.trim().isEmpty()) {
            return;
        }

        if (mName.trim().length() > 32) {
            return;
        }

        Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(playerEntity.getUUID());
        if (faction == null) {
            return;
        }

        if (!faction.isPlayerRoleInFaction(playerEntity.getUUID(), Faction.Role.OFFICER)) {
            return;
        }

        TileEntityFob te = PacketSecurity.blockEntityAt(playerEntity, mPos, TileEntityFob.class, "establish FOB");
        if (te != null) {
            WarForgeMod.FOBS.requestCreateFob(te, playerEntity, mName);
        }
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Recieved establish FOB message on client");
    }
}
