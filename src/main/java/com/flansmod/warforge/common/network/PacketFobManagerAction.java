package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.Role;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.FobBlockEntity;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.fob.Fob;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketFobManagerAction extends ServerboundPacket {
    public Action action = Action.WARP;
    public DimBlockPos target = DimBlockPos.ZERO;
    public String name = "";

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeByte(action.ordinal());
        writeUTF(data, Keys.id(target.dim).toString());
        data.writeInt(target.getX());
        data.writeInt(target.getY());
        data.writeInt(target.getZ());
        writeUTF(data, name);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        action = PacketSecurity.readEnum(data, Action.values(), Action.WARP);
        ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        target = new DimBlockPos(dim, x, y, z);
        name = readUTF(data, PacketSecurity.MAX_FOB_NAME);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        switch (action) {
            case ESTABLISH -> handleEstablish(playerEntity);
            case WARP -> handleWarp(playerEntity);
        }
    }

    private boolean handleEstablish(ServerPlayer playerEntity) {
        if (!PacketSecurity.requireReach(playerEntity, target, "establish FOB")) {
            return false;
        }

        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        if (name.trim().length() > PacketSecurity.MAX_FOB_NAME) {
            return false;
        }

        Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(playerEntity.getUUID());
        if (faction == null) {
            return false;
        }

        if (!faction.isPlayerRoleInFaction(playerEntity.getUUID(), Role.OFFICER)) {
            return false;
        }

        FobBlockEntity te = PacketSecurity.blockEntityAt(playerEntity, target, FobBlockEntity.class, "establish FOB");
        if (te != null) {
            WarForgeMod.FOBS.requestCreateFob(te, playerEntity, name);
            return true;
        }
        return false;
    }

    private boolean handleWarp(ServerPlayer playerEntity) {
        if (!PacketSecurity.requireSameDim(playerEntity, target.dim, "FOB warp")) {
            return false;
        }
        Fob fob = WarForgeMod.FOBS.getFobAt(target.toChunkPos());
        if (fob == null) {
            return false;
        }
        WarForgeMod.FOBS.requestFobWarp(playerEntity, fob);
        return true;
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
    }

    public enum Action {
        ESTABLISH,
        WARP
    }
}
