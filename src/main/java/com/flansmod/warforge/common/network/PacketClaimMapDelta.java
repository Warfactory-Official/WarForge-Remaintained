package com.flansmod.warforge.common.network;

import com.flansmod.warforge.compat.Keys;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketClaimMapDelta extends PacketBase {
    public static final int MAX_ENTRIES = 4000;
    private static final int MAX_OWNERS = 1 << 12;

    public record Set(ResourceKey<Level> dim, int x, int z, UUID owner, int colour) {
    }

    public record Remove(ResourceKey<Level> dim, int x, int z) {
    }

    public final List<Set> sets = new ArrayList<>();
    public final List<Remove> removes = new ArrayList<>();

    public int size() {
        return sets.size() + removes.size();
    }

    public boolean isFull() {
        return size() >= MAX_ENTRIES;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        Object2IntLinkedOpenHashMap<UUID> ownerIndex = new Object2IntLinkedOpenHashMap<>();
        ownerIndex.defaultReturnValue(-1);
        IntArrayList colours = new IntArrayList();
        for (Set set : sets) {
            if (ownerIndex.putIfAbsent(set.owner, ownerIndex.size()) == -1) {
                colours.add(set.colour);
            }
        }
        data.writeInt(ownerIndex.size());
        int i = 0;
        for (UUID owner : ownerIndex.keySet()) {
            writeUUID(data, owner);
            data.writeInt(colours.getInt(i++));
        }
        data.writeInt(sets.size());
        for (Set set : sets) {
            data.writeUtf(Keys.id(set.dim).toString());
            data.writeInt(set.x);
            data.writeInt(set.z);
            data.writeVarInt(ownerIndex.getInt(set.owner));
        }
        data.writeInt(removes.size());
        for (Remove remove : removes) {
            data.writeUtf(Keys.id(remove.dim).toString());
            data.writeInt(remove.x);
            data.writeInt(remove.z);
        }
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        int ownerCount = PacketSecurity.readCount(data, MAX_OWNERS);
        UUID[] owners = new UUID[ownerCount];
        int[] colours = new int[ownerCount];
        for (int i = 0; i < ownerCount; i++) {
            owners[i] = readUUID(data);
            colours[i] = data.readInt();
        }
        int setCount = PacketSecurity.readCount(data, MAX_ENTRIES);
        for (int i = 0; i < setCount; i++) {
            ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
            int x = data.readInt();
            int z = data.readInt();
            int owner = data.readVarInt();
            sets.add(new Set(dim, x, z, owners[owner], colours[owner]));
        }
        int removeCount = PacketSecurity.readCount(data, MAX_ENTRIES);
        for (int i = 0; i < removeCount; i++) {
            ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
            removes.add(new Remove(dim, data.readInt(), data.readInt()));
        }
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
    }
}
