package com.flansmod.warforge.common.factories;

import com.cleanroommc.modularui.factory.GuiData;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.network.PacketClaimChunksData;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClaimManagerGuiData extends GuiData {
    public final int dim;
    public final int centerX;
    public final int centerZ;
    public final int radius;
    public final int pageX;
    public final int pageZ;
    public final UUID playerFactionId;
    public final int forceLoadedCount;
    public final int forceLoadedMax;
    public final int claimCount;
    public final int claimMax;
    public final List<ClaimChunkInfo> chunks;

    public ClaimManagerGuiData(EntityPlayer player, DimChunkPos center, int radius, int pageX, int pageZ) {
        super(player);
        this.dim = center.dim;
        this.centerX = center.x;
        this.centerZ = center.z;
        this.radius = radius;
        this.pageX = pageX;
        this.pageZ = pageZ;
        this.playerFactionId = Faction.nullUuid;
        this.forceLoadedCount = 0;
        this.forceLoadedMax = 0;
        this.claimCount = 0;
        this.claimMax = 0;
        this.chunks = new ArrayList<ClaimChunkInfo>();
    }

    public ClaimManagerGuiData(EntityPlayer player, PacketClaimChunksData packet, int pageX, int pageZ) {
        super(player);
        this.dim = packet.dim;
        this.centerX = packet.centerX;
        this.centerZ = packet.centerZ;
        this.radius = packet.radius;
        this.pageX = pageX;
        this.pageZ = pageZ;
        this.playerFactionId = packet.playerFactionId;
        this.forceLoadedCount = packet.forceLoadedCount;
        this.forceLoadedMax = packet.forceLoadedMax;
        this.claimCount = packet.claimCount;
        this.claimMax = packet.claimMax;
        this.chunks = new ArrayList<ClaimChunkInfo>(packet.chunks);
    }

    public DimChunkPos getCenter() {
        return new DimChunkPos(dim, centerX, centerZ);
    }

    public PacketClaimChunksData toPacket() {
        PacketClaimChunksData packet = new PacketClaimChunksData();
        packet.dim = dim;
        packet.centerX = centerX;
        packet.centerZ = centerZ;
        packet.radius = radius;
        packet.playerFactionId = playerFactionId;
        packet.forceLoadedCount = forceLoadedCount;
        packet.forceLoadedMax = forceLoadedMax;
        packet.claimCount = claimCount;
        packet.claimMax = claimMax;
        packet.chunks.addAll(chunks);
        return packet;
    }
}
