package com.flansmod.warforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@OnlyIn(Dist.CLIENT)
public class ModelEventHandler {

    @SubscribeEvent
    public void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        event.register(PoleFlagRenderer.POLE_MODEL);
    }
}
