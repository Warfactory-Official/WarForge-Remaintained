package com.flansmod.warforge.platform;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

public final class Platform {
    private Platform() {}

    public static boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    public static boolean isDevelopment() {
        return !FMLEnvironment.production;
    }

    public static IEventBus gameBus() {
        return NeoForge.EVENT_BUS;
    }

    public static java.nio.file.Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
