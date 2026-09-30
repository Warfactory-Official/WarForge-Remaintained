package com.flansmod.warforge.platform;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

/** Loader facts and the game event bus. */
public final class Platform {
    private Platform() {}

    /** True on the physical client (also true for the integrated server of a single-player game). */
    public static boolean isClient() {
        return FMLEnvironment.getDist().isClient();
    }

    /** The game event bus (NeoForge.EVENT_BUS / MinecraftForge.EVENT_BUS). */
    public static IEventBus gameBus() {
        return NeoForge.EVENT_BUS;
    }
}
