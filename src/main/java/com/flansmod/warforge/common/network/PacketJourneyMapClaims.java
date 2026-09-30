package com.flansmod.warforge.common.network;

import com.flansmod.warforge.client.MinimapClaimCache;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import com.flansmod.warforge.compat.Keys;

/**
 * Server -> client claim-border updates for the optional JourneyMap overlay.
 * <p>
 * Carries only chunk position + faction colour (never faction names/ids/members). The server decides
 * what each client is allowed to receive based on {@code JOURNEYMAP_CLAIM_MODE}, so a client cannot
 * learn about claims it is not entitled to see.
 */
public class PacketJourneyMapClaims extends PacketBase {
    public static final class Set {
        public final ResourceKey<Level> dim;
        public final int x, z, colour;
        public Set(ResourceKey<Level> dim, int x, int z, int colour) { this.dim = dim; this.x = x; this.z = z; this.colour = colour; }
    }

    public static final class Remove {
        public final ResourceKey<Level> dim;
        public final int x, z;
        public Remove(ResourceKey<Level> dim, int x, int z) { this.dim = dim; this.x = x; this.z = z; }
    }

    /** When true the client wipes its claim cache before applying this packet (used for a full snapshot). */
    public boolean clear = false;
    public final List<Set> sets = new ArrayList<>();
    public final List<Remove> removes = new ArrayList<>();

    public void addSet(ResourceKey<Level> dim, int x, int z, int colour) {
        sets.add(new Set(dim, x, z, colour));
    }

    public void addRemove(ResourceKey<Level> dim, int x, int z) {
        removes.add(new Remove(dim, x, z));
    }

    public boolean isEmpty() {
        return !clear && sets.isEmpty() && removes.isEmpty();
    }

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeBoolean(clear);
        data.writeInt(sets.size());
        for (Set s : sets) {
            data.writeUtf(Keys.id(s.dim).toString());
            data.writeInt(s.x);
            data.writeInt(s.z);
            data.writeInt(s.colour);
        }
        data.writeInt(removes.size());
        for (Remove r : removes) {
            data.writeUtf(Keys.id(r.dim).toString());
            data.writeInt(r.x);
            data.writeInt(r.z);
        }
    }

    /** Each entry costs at least 12 bytes on the wire; well past any real sync batch. */
    private static final int MAX_ENTRIES = 1 << 16;

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        clear = data.readBoolean();
        int setCount = PacketSecurity.readCount(data, MAX_ENTRIES);
        for (int i = 0; i < setCount; i++) {
            ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
            sets.add(new Set(dim, data.readInt(), data.readInt(), data.readInt()));
        }
        int removeCount = PacketSecurity.readCount(data, MAX_ENTRIES);
        for (int i = 0; i < removeCount; i++) {
            ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
            removes.add(new Remove(dim, data.readInt(), data.readInt()));
        }
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        // client-bound only
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        if (clear) {
            MinimapClaimCache.applyClear();
        }
        for (Set s : sets) {
            MinimapClaimCache.set(s.dim, s.x, s.z, s.colour);
        }
        for (Remove r : removes) {
            MinimapClaimCache.remove(r.dim, r.x, r.z);
        }
    }
}
