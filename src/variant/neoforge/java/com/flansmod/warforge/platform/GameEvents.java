package com.flansmod.warforge.platform;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.Consumer;

/** Game events whose loader event classes differ between versions. Handlers get neutral types. */
public final class GameEvents {
    private GameEvents() {}

    /** At the end of every server tick. */
    public static void onServerTickEnd(Runnable handler) {
        Platform.gameBus().addListener((ServerTickEvent.Post event) -> handler.run());
    }

    /** At the end of every client tick (client only). */
    public static void onClientTickEnd(EventPriority priority, Runnable handler) {
        Platform.gameBus().addListener(priority, (ClientTickEvent.Post event) -> handler.run());
    }

    public static void onLivingDamage(Consumer<LivingDamage> handler) {
        Platform.gameBus().addListener((LivingIncomingDamageEvent event) -> handler.accept(new LivingDamage() {
            @Override
            public net.minecraft.world.entity.LivingEntity getEntity() {
                return event.getEntity();
            }

            @Override
            public net.minecraft.world.damagesource.DamageSource getSource() {
                return event.getSource();
            }

            @Override
            public void setCanceled(boolean canceled) {
                event.setCanceled(canceled);
            }
        }));
    }

    public static void onBlockBreak(Consumer<BlockBreak> handler) {
        BreakEvents.register(handler);
    }
}
