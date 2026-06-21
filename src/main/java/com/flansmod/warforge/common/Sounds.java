package com.flansmod.warforge.common;

import com.flansmod.warforge.Tags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Sounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Tags.MODID);

    public static final RegistryObject<SoundEvent> SFX_UPGRADE =
            SOUND_EVENTS.register("sfx.upgrade",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Tags.MODID, "sfx.upgrade")));

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}
