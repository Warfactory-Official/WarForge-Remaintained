package com.flansmod.warforge.common;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.PoleFlagRenderer;
import com.flansmod.warforge.compat.Ids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;

public class ModelEventHandler {

    @SubscribeEvent
    public void onRegisterStandalone(ModelEvent.RegisterStandalone event) {
        event.register(PoleFlagRenderer.POLE_MODEL, SimpleUnbakedStandaloneModel.quadCollection(Ids.of(Tags.MODID, "block/pole")));
    }
}
