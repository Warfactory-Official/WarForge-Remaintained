package com.flansmod.warforge.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.function.Consumer;

/** The block-break event (its class name differs between versions). Used by GameEvents. */
final class BreakEvents {
    private BreakEvents() {}

    static void register(Consumer<BlockBreak> handler) {
        Platform.gameBus().addListener((BreakBlockEvent event) -> handler.accept(new BlockBreak() {
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
