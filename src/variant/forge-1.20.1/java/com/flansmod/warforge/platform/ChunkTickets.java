package com.flansmod.warforge.platform;

import com.flansmod.warforge.Tags;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Force-loaded chunks owned by a UUID, which the loader saves and restores across restarts. */
public final class ChunkTickets {
    /** The saved tickets of one owner in one level, handed to the validator on world load. */
    public interface OwnerTickets {
        /** Packed chunk positions (ChunkPos.getX/getZ) of the ticking tickets. */
        LongSet ticking();

        /** Packed chunk positions of the non-ticking tickets. */
        LongSet nonTicking();
    }

    /** Checks the saved tickets when a level loads; may remove them through {@link Remover}. */
    public interface Validator {
        void validate(ServerLevel level, Map<UUID, OwnerTickets> ticketsByOwner, Remover remover);
    }

    public interface Remover {
        void removeAll(UUID owner);

        void remove(UUID owner, long packedChunk, boolean ticking);
    }

    private ChunkTickets() {
    }

    /** Forge keys tickets by mod id, so id is only a name here. */
    public static ChunkTickets create(IEventBus modBus, String id, Validator validator) {
        ForgeChunkManager.setForcedChunkLoadingCallback(Tags.MODID, (level, helper) -> {
            Map<UUID, OwnerTickets> byOwner = new LinkedHashMap<>();
            for (Map.Entry<UUID, Pair<LongSet, LongSet>> entry : helper.getEntityTickets().entrySet()) {
                // First element: non-ticking tickets; second element: ticking tickets.
                Pair<LongSet, LongSet> sets = entry.getValue();
                byOwner.put(entry.getKey(), new OwnerTickets() {
                    @Override
                    public LongSet ticking() {
                        return sets.getSecond();
                    }

                    @Override
                    public LongSet nonTicking() {
                        return sets.getFirst();
                    }
                });
            }
            validator.validate(level, byOwner, new Remover() {
                @Override
                public void removeAll(UUID owner) {
                    helper.removeAllTickets(owner);
                }

                @Override
                public void remove(UUID owner, long packedChunk, boolean ticking) {
                    helper.removeTicket(owner, packedChunk, ticking);
                }
            });
        });
        return new ChunkTickets();
    }

    public void force(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        ForgeChunkManager.forceChunk(level, Tags.MODID, owner, chunkX, chunkZ, add, ticking);
    }
}
