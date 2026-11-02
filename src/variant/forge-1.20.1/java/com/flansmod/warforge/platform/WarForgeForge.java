package com.flansmod.warforge.platform;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.ClientConfig;
import com.flansmod.warforge.client.WarForgeClientInit;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Tags.MODID)
public final class WarForgeForge {
    public WarForgeForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WarForgeConfig.SPEC);
        if (Platform.isClient()) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        }
        new WarForgeMod(FMLJavaModLoadingContext.get().getModEventBus());
        if (Platform.isClient()) {
            WarForgeClientInit.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
