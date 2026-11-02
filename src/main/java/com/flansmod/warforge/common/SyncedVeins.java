package com.flansmod.warforge.common;

import com.flansmod.warforge.api.vein.Vein;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

/** Server veins as received by a client. */
public final class SyncedVeins {
    public static final Short2ObjectOpenHashMap<Vein> ENTRIES = new Short2ObjectOpenHashMap<>();
    public static short megachunkLength = -1;

    private SyncedVeins() {}
}
