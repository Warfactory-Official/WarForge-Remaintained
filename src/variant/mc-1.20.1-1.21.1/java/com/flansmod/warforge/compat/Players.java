package com.flansmod.warforge.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Player accessors whose names or types differ between versions. */
public final class Players {
    private Players() {}

    public static ServerLevel serverLevel(ServerPlayer player) {
        return player.serverLevel();
    }

    /** A command source acting as the player (server side). */
    public static CommandSourceStack commandSource(Player player) {
        return player.createCommandSourceStack();
    }

    /** The player's bed/anchor respawn position, or null if none is set. */
    public static @Nullable BlockPos respawnPosition(ServerPlayer player) {
        return player.getRespawnPosition();
    }

    /** The dimension of the player's respawn position. */
    public static ResourceKey<Level> respawnDimension(ServerPlayer player) {
        return player.getRespawnDimension();
    }
}
