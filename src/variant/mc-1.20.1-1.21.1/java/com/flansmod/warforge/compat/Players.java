package com.flansmod.warforge.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class Players {
    private Players() {}

    public static ServerLevel serverLevel(ServerPlayer player) {
        return player.serverLevel();
    }

    public static CommandSourceStack commandSource(ServerPlayer player) {
        return player.createCommandSourceStack();
    }

    public static @Nullable BlockPos respawnPosition(ServerPlayer player) {
        return player.getRespawnPosition();
    }

    public static ResourceKey<Level> respawnDimension(ServerPlayer player) {
        return player.getRespawnDimension();
    }
}
