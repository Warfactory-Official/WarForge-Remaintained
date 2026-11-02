package com.flansmod.warforge.platform;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.compat.Ids;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;

import java.util.Map;
import java.util.UUID;

public final class ChunkTickets {
    public interface OwnerTickets {
        LongSet ticking();

        LongSet nonTicking();
    }

    public interface Validator {
        void validate(ServerLevel level, Map<UUID, OwnerTickets> ticketsByOwner, Remover remover);
    }

    public interface Remover {
        void removeAll(UUID owner);

        void remove(UUID owner, long packedChunk, boolean ticking);
    }

    private final TicketController controller;

    private ChunkTickets(TicketController controller) {
        this.controller = controller;
    }

    public static ChunkTickets create(IEventBus modBus, String id, Validator validator) {
        TicketController controller = new TicketController(Ids.of(Tags.MODID, id),
                (level, helper) -> validator.validate(level, wrap(helper), remover(helper)));
        modBus.addListener((RegisterTicketControllersEvent event) -> event.register(controller));
        return new ChunkTickets(controller);
    }

    public void force(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        controller.forceChunk(level, owner, chunkX, chunkZ, add, ticking);
    }

    private static Map<UUID, OwnerTickets> wrap(TicketHelper helper) {
        Map<UUID, OwnerTickets> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<UUID, TicketSet> entry : helper.getEntityTickets().entrySet()) {
            TicketSet set = entry.getValue();
            out.put(entry.getKey(), new OwnerTickets() {
                @Override
                public LongSet ticking() {
                    return TicketSets.ticking(set);
                }

                @Override
                public LongSet nonTicking() {
                    return TicketSets.nonTicking(set);
                }
            });
        }
        return out;
    }

    private static Remover remover(TicketHelper helper) {
        return new Remover() {
            @Override
            public void removeAll(UUID owner) {
                helper.removeAllTickets(owner);
            }

            @Override
            public void remove(UUID owner, long packedChunk, boolean ticking) {
                helper.removeTicket(owner, packedChunk, ticking);
            }
        };
    }
}
