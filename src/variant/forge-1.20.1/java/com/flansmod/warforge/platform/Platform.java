package com.flansmod.warforge.platform;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

public final class Platform {
    private Platform() {}

    public static boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    public static boolean isDevelopment() {
        return !FMLEnvironment.production;
    }

    public static IEventBus gameBus() {
        return MinecraftForge.EVENT_BUS;
    }

    public static java.nio.file.Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
