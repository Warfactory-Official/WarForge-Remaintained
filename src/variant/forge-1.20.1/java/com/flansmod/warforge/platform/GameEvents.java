package com.flansmod.warforge.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.function.Consumer;

/** Game events whose loader event classes differ between versions. Handlers get neutral types. */
public final class GameEvents {
    private GameEvents() {}

    /** At the end of every server tick. */
    public static void onServerTickEnd(Runnable handler) {
        Platform.gameBus().addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) handler.run();
        });
    }

    /** At the end of every client tick (client only). */
    public static void onClientTickEnd(EventPriority priority, Runnable handler) {
        Platform.gameBus().addListener(priority, (TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) handler.run();
        });
    }

    public static void onLivingDamage(Consumer<LivingDamage> handler) {
        Platform.gameBus().addListener((LivingAttackEvent event) -> handler.accept(new LivingDamage() {
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
        Platform.gameBus().addListener((BlockEvent.BreakEvent event) -> handler.accept(new BlockBreak() {
            @Override
            public LevelAccessor getLevel() {
                return event.getLevel();
            }

            @Override
            public BlockPos getPos() {
                return event.getPos();
            }

            @Override
            public BlockState getState() {
                return event.getState();
            }

            @Override
            public Player getPlayer() {
                return event.getPlayer();
            }

            @Override
            public void setCanceled(boolean canceled) {
                event.setCanceled(canceled);
            }
        }));
    }
}
