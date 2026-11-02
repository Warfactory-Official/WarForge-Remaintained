package com.flansmod.warforge.platform;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.ClientConfig;
import com.flansmod.warforge.client.WarForgeClientInit;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Tags.MODID)
public final class WarForgeNeoForge {
    public WarForgeNeoForge(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, WarForgeConfig.SPEC);
        if (Platform.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        }
        new WarForgeMod(modBus);
        if (Platform.isClient()) {
            WarForgeClientInit.init(modBus);
        }
    }
}
