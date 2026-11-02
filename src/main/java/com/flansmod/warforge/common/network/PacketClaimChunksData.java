package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.SyncedVeins;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.common.ProtectionProfile;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.compat.Keys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketClaimChunksData extends PacketBase {
    public ResourceKey<Level> dim = Level.OVERWORLD;
    public UUID playerFactionId = FactionIds.NONE;
    public int forceLoadedCount;
    public int forceLoadedMax;
    public int claimCount;
    public int claimMax;
    /** Window the chunks were collected for; an outline sync replaces exactly this square. */
    public int centerX;
    public int centerZ;
    public int radius;
    public boolean outlineOnly = false;
    public List<ClaimChunkInfo> chunks = new ArrayList<ClaimChunkInfo>();

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeUtf(Keys.id(dim).toString());
        data.writeInt(centerX);
        data.writeInt(centerZ);
        data.writeByte(radius);
        data.writeBoolean(outlineOnly);
        writeUUID(data, playerFactionId);
        data.writeShort(forceLoadedCount);
        data.writeShort(forceLoadedMax);
        data.writeShort(claimCount);
        data.writeShort(claimMax);

        data.writeShort(chunks.size());
        for (ClaimChunkInfo chunk : chunks) {
            data.writeInt(chunk.x);
            data.writeInt(chunk.z);
            writeUUID(data, chunk.factionId);
            writeUTF(data, chunk.factionName);
            writeUTF(data, chunk.flagId);
            data.writeInt(chunk.colour);
            writeUTF(data, chunk.claimType.serializedName);
            data.writeShort(chunk.vein != null ? chunk.vein.getId() : -1);
            data.writeByte(chunk.oreQuality != null ? chunk.oreQuality.ordinal() : -1);
            data.writeByte(chunk.flags);
            writeUUID(data, chunk.outlineFactionId);
            data.writeInt(chunk.outlineColour);
            data.writeByte(chunk.outlineStyle);
            data.writeInt(chunk.conqueredRemainingMs);
            data.writeByte(chunk.protection.ordinal());
        }
    }

    /** The outline sync is the widest window the server ever serves: (2 * 48 + 1)^2. */
    private static final int MAX_CHUNKS = 9409;

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        dim = PacketSecurity.readDimOrOverworld(data);
        centerX = data.readInt();
        centerZ = data.readInt();
        radius = data.readByte();
        outlineOnly = data.readBoolean();
        playerFactionId = readUUID(data);
        forceLoadedCount = data.readShort();
        forceLoadedMax = data.readShort();
        claimCount = data.readShort();
        claimMax = data.readShort();

        chunks.clear();
        int size = PacketSecurity.clampCount(data.readShort(), MAX_CHUNKS);
        for (int i = 0; i < size; i++) {
            ClaimChunkInfo info = new ClaimChunkInfo();
            info.x = data.readInt();
            info.z = data.readInt();
            info.factionId = readUUID(data);
            info.factionName = readUTF(data, PacketSecurity.MAX_NAME);
            info.flagId = readUTF(data, PacketSecurity.MAX_FLAG_ID);
            info.colour = data.readInt();
            info.claimType = ClaimType.fromSerialized(readUTF(data, PacketSecurity.MAX_NAME));
            short veinId = data.readShort();
            info.vein = veinId < 0 ? null : SyncedVeins.ENTRIES.get(veinId);
            int qualityOrdinal = data.readByte();
            info.oreQuality = (qualityOrdinal < 0 || qualityOrdinal >= Quality.values().length)
                    ? null : Quality.values()[qualityOrdinal];
            info.flags = data.readByte();
            info.outlineFactionId = readUUID(data);
            info.outlineColour = data.readInt();
            info.outlineStyle = data.readByte();
            info.conqueredRemainingMs = data.readInt();
            info.protection = ProtectionProfile.fromId(data.readByte());
            chunks.add(info);
        }
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
    }
}
