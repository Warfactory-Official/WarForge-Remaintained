package com.flansmod.warforge.server;

import com.flansmod.warforge.api.ObjectIntPair;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;
import com.flansmod.warforge.compat.Nbt;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Post-siege no-claim / no-siege grace entries: chunk -> (cosmetic faction, remaining ms). */
public final class ConqueredChunks {
    private final HashMap<DimChunkPos, ObjectIntPair<UUID>> chunks = new HashMap<>();

    public boolean isConquered(DimChunkPos chunk) {
        return chunks.containsKey(chunk);
    }

    /** Unclaimed conquered chunk: reverting no-man's-land, unclaimable and unprotected. */
    public boolean isConqueredWilderness(DimChunkPos chunk, ClaimRegistry claims) {
        return chunks.containsKey(chunk) && !claims.isClaimed(chunk);
    }

    public int getRemainingMs(DimChunkPos chunk) {
        ObjectIntPair<UUID> entry = chunks.get(chunk);
        return entry == null ? 0 : entry.getInteger();
    }

    /** @return the faction credited with the conquest, or {@link FactionIds#NONE} */
    public UUID getFaction(DimChunkPos chunk) {
        ObjectIntPair<UUID> entry = chunks.get(chunk);
        return entry == null ? FactionIds.NONE : entry.getObj();
    }

    public void set(DimChunkPos chunk, UUID factionId, int periodMs) {
        chunks.put(chunk, new ObjectIntPair<>(factionId == null ? FactionIds.NONE : factionId, periodMs));
    }

    public boolean clear(DimChunkPos chunk) {
        return chunks.remove(chunk) != null;
    }

    /** The sieged chunk and every attacking camp chunk; no-op when {@code periodMs <= 0}. */
    void mark(Siege siege, UUID factionId, int periodMs) {
        if (periodMs <= 0) {
            return;
        }
        set(siege.defendingClaim.toChunkPos(), factionId, periodMs);
        for (DimBlockPos camp : siege.attackingCamps) {
            if (camp != null) {
                set(camp.toChunkPos(), factionId, periodMs);
            }
        }
    }

    /** Disband: entries credited to the faction are dropped. */
    void forgetFaction(UUID factionId) {
        chunks.values().removeIf(entry -> factionId.equals(entry.getObj()));
    }

    void tick(int msPassed) {
        Iterator<ObjectIntPair<UUID>> it = chunks.values().iterator();
        while (it.hasNext()) {
            ObjectIntPair<UUID> entry = it.next();
            if (entry.getInteger() < msPassed) {
                it.remove();
            } else {
                entry.setInteger(entry.getInteger() - msPassed);
            }
        }
    }

    public Map<DimChunkPos, ObjectIntPair<UUID>> entries() {
        return java.util.Collections.unmodifiableMap(chunks);
    }

    void readFromNBT(CompoundTag tags) {
        chunks.clear();
        CompoundTag list = Nbt.getCompound(tags, "conqueredChunks");
        for (int index = 0; ; index++) {
            String key = "conqueredChunk_" + index;
            if (Nbt.tagType(list, key) != Tag.TAG_COMPOUND) {
                break;
            }
            CompoundTag entry = Nbt.getCompound(list, key);
            int[] chunkXZ = Nbt.getIntArray(entry, "pos");
            int[] factionArr = Nbt.getIntArray(entry, "faction");
            if (chunkXZ.length < 2 || factionArr.length < 4) {
                WarForgeMod.LOGGER.warn("Skipping malformed conqueredChunk entry at index {}", index);
                continue;
            }
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, Ids.parse(Nbt.getString(entry, "dim")));
            chunks.put(new DimChunkPos(dim, chunkXZ[0], chunkXZ[1]),
                    new ObjectIntPair<>(UUIDUtil.uuidFromIntArray(factionArr), Nbt.getInt(entry, "period")));
        }
    }

    void writeToNBT(CompoundTag tags) {
        CompoundTag list = new CompoundTag();
        int index = 0;
        for (Map.Entry<DimChunkPos, ObjectIntPair<UUID>> conquered : chunks.entrySet()) {
            DimChunkPos chunk = conquered.getKey();
            CompoundTag entry = new CompoundTag();
            entry.putString("dim", Keys.id(chunk.dim).toString());
            entry.putIntArray("pos", new int[]{chunk.x, chunk.z});
            entry.putIntArray("faction", UUIDUtil.uuidToIntArray(conquered.getValue().getObj()));
            entry.putInt("period", conquered.getValue().getInteger());
            list.put("conqueredChunk_" + index++, entry);
        }
        tags.put("conqueredChunks", list);
    }
}
