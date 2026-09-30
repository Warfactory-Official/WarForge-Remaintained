package com.flansmod.warforge.client;

import com.flansmod.warforge.client.particle.ParticleStarCircle;
import com.flansmod.warforge.common.Content;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class ClientContent {

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Content.STAR_CIRCLE_PARTICLE.get(), ParticleStarCircle.Provider::new);
    }
}
