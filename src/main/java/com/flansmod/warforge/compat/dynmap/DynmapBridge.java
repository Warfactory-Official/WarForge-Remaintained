package com.flansmod.warforge.compat.dynmap;

import com.flansmod.warforge.server.FactionStorage;

/** Dynmap-free entry point; {@link DynmapLayer} loads only after {@link #start}. */
public final class DynmapBridge {
    private static boolean active;

    private DynmapBridge() {
    }

    public static void start(FactionStorage hub) {
        active = true;
        DynmapLayer.start(hub);
    }

    public static void stop() {
        if (active) {
            active = false;
            DynmapLayer.stop();
        }
    }

    public static void tick() {
        if (active) {
            DynmapLayer.tick();
        }
    }
}
