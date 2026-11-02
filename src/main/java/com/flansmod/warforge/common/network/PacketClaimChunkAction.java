package com.flansmod.warforge.common.network;

import com.flansmod.warforge.server.ClaimMapView;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketClaimChunkAction extends ServerboundPacket {
    public static final byte ACTION_CLAIM = 0;
    public static final byte ACTION_UNCLAIM = 1;
    public static final byte ACTION_TOGGLE_FORCELOAD = 2;

    public DimChunkPos chunk = new DimChunkPos(Level.OVERWORLD, 0, 0);
    public DimChunkPos center = new DimChunkPos(Level.OVERWORLD, 0, 0);
    public int radius = 4;
    public byte action = ACTION_CLAIM;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeUtf(Keys.id(chunk.dim).toString());
        data.writeInt(chunk.x);
        data.writeInt(chunk.z);
        data.writeByte(action);
        data.writeUtf(Keys.id(center.dim).toString());
        data.writeInt(center.x);
        data.writeInt(center.z);
        data.writeByte(radius);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        ResourceKey<Level> chunkDim = PacketSecurity.readDimOrOverworld(data);
        chunk = new DimChunkPos(chunkDim, data.readInt(), data.readInt());
        action = data.readByte();
        ResourceKey<Level> centerDim = PacketSecurity.readDimOrOverworld(data);
        center = new DimChunkPos(centerDim, data.readInt(), data.readInt());
        radius = data.readByte();
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        if (action != ACTION_CLAIM && action != ACTION_UNCLAIM && action != ACTION_TOGGLE_FORCELOAD) {
            PacketSecurity.violation(playerEntity, "unknown claim action " + action);
            return;
        }

        switch (action) {
            case ACTION_CLAIM:
                WarForgeMod.FACTIONS.claimService.requestClaimChunk(playerEntity, chunk);
                break;
            case ACTION_UNCLAIM:
                WarForgeMod.FACTIONS.claimService.requestRemoveClaimByChunk(playerEntity, chunk);
                break;
            case ACTION_TOGGLE_FORCELOAD:
                WarForgeMod.FACTIONS.islands.requestToggleForceLoad(playerEntity, chunk);
                break;
            default:
                break;
        }

        int clampedRadius = Math.max(1, Math.min(radius, WarForgeConfig.CLAIM_MANAGER_RADIUS));
        ClaimMapView.send(playerEntity, center, clampedRadius);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
    }

    @Override
    public DimChunkPos target() {
        return chunk;
    }
}
