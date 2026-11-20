package com.flansmod.warforge.platform;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.Consumer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class GameEvents {
    private GameEvents() {}

    public static void onServerTickEnd(Runnable handler) {
        Platform.gameBus().addListener((ServerTickEvent.Post event) -> handler.run());
    }

    public static void onLivingDamage(Consumer<LivingDamage> handler) {
        Platform.gameBus().addListener((LivingIncomingDamageEvent event) -> handler.accept(new LivingDamage() {
            @Override
            public LivingEntity getEntity() {
                return event.getEntity();
            }

            @Override
            public DamageSource getSource() {
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
