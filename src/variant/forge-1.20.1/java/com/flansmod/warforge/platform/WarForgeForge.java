package com.flansmod.warforge.platform;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge entry point. Everything else is loader-neutral and starts in WarForgeMod. */
@Mod(Tags.MODID)
public final class WarForgeForge {
    public WarForgeForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WarForgeConfig.SPEC);
        new WarForgeMod(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
