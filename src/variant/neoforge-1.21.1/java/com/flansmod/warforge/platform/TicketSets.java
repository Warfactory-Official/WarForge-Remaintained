package com.flansmod.warforge.platform;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.neoforged.neoforge.common.world.chunk.TicketSet;

/** TicketSet accessors. NeoForge 26 renamed ticking/nonTicking to naturalSpawning/normal. */
final class TicketSets {
    private TicketSets() {}

    static LongSet ticking(TicketSet set) {
        return set.ticking();
    }

    static LongSet nonTicking(TicketSet set) {
        return set.nonTicking();
    }
}
