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
        return player.level();
    }

    public static CommandSourceStack commandSource(ServerPlayer player) {
        return player.createCommandSourceStack();
    }

    public static @Nullable BlockPos respawnPosition(ServerPlayer player) {
        ServerPlayer.RespawnConfig config = player.getRespawnConfig();
        return config == null ? null : config.respawnData().globalPos().pos();
    }

    public static ResourceKey<Level> respawnDimension(ServerPlayer player) {
        ServerPlayer.RespawnConfig config = player.getRespawnConfig();
        return config == null ? Level.OVERWORLD : config.respawnData().globalPos().dimension();
    }
}
