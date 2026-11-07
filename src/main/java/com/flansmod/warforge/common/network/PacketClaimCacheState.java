package com.flansmod.warforge.common.network;

import com.flansmod.warforge.compat.Keys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client claim state, written into Flashback snapshots; never sent by a server. {@code first} starts a series and
 * replaces all state. Decoded only on the replay server.
 */
public class PacketClaimCacheState extends PacketBase {
    public static final int MAX_ENTRIES = 4000;
    /** Initial snapshot plays before the viewer connects => Flashback forwards it to nobody. Applied at login instead. */
    private static final List<PacketClaimCacheState> DECODED = new ArrayList<>();

    public record Entry(int x, int z, UUID owner, int colour, UUID outlineOwner, int outlineColour, byte outlineStyle) {
    }

    public boolean first;
    public ResourceKey<Level> dim = Level.OVERWORLD;
    public final List<Entry> entries = new ArrayList<>();

    public static List<PacketClaimCacheState> decoded() {
        synchronized (DECODED) {
            return new ArrayList<>(DECODED);
        }
    }

    public static void clearDecoded() {
        synchronized (DECODED) {
            DECODED.clear();
        }
    }

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeBoolean(first);
        data.writeUtf(Keys.id(dim).toString());
        data.writeInt(entries.size());
        for (Entry entry : entries) {
            data.writeInt(entry.x);
            data.writeInt(entry.z);
            writeUUID(data, entry.owner);
            data.writeInt(entry.colour);
            writeUUID(data, entry.outlineOwner);
            data.writeInt(entry.outlineColour);
            data.writeByte(entry.outlineStyle);
        }
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        first = data.readBoolean();
        dim = PacketSecurity.readDimOrOverworld(data);
        int count = PacketSecurity.readCount(data, MAX_ENTRIES);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(data.readInt(), data.readInt(), readUUID(data), data.readInt(), readUUID(data),
                    data.readInt(), data.readByte()));
        }
        synchronized (DECODED) {
            if (first) {
                DECODED.clear();
            }
            DECODED.add(this);
        }
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
    }
}
