package com.flansmod.warforge.platform;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.neoforged.neoforge.common.world.chunk.TicketSet;

final class TicketSets {
    private TicketSets() {}

    static LongSet ticking(TicketSet set) {
        return set.naturalSpawning();
    }

    static LongSet nonTicking(TicketSet set) {
        return set.normal();
    }
}
