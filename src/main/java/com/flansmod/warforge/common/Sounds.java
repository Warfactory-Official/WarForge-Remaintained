package com.flansmod.warforge.common;

import com.flansmod.warforge.Tags;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import com.flansmod.warforge.platform.RegHandle;
import com.flansmod.warforge.platform.WFRegistry;
import com.flansmod.warforge.compat.Ids;

public class Sounds {
    private static final WFRegistry REG = new WFRegistry();

    public static final RegHandle<SoundEvent> SFX_UPGRADE =
            REG.register(Registries.SOUND_EVENT, "sfx.upgrade",
                    () -> SoundEvent.createVariableRangeEvent(Ids.of(Tags.MODID, "sfx.upgrade")));

    public static void register(IEventBus modBus) {
        REG.register(modBus);
    }
}
