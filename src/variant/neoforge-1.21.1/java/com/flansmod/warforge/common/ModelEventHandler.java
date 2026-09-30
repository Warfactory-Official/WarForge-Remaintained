package com.flansmod.warforge.common;

import com.flansmod.warforge.Tags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import com.flansmod.warforge.client.PoleFlagRenderer;

@OnlyIn(Dist.CLIENT)
public class ModelEventHandler {

    @SubscribeEvent
    public void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        event.register(PoleFlagRenderer.POLE_MODEL);
    }
}
