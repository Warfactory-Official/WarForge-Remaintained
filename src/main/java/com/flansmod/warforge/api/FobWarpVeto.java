package com.flansmod.warforge.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/** Registered through {@link WarforgeAPI#addFobWarpVeto}; consulted on the server thread before a FOB warp is queued. */
@FunctionalInterface
public interface FobWarpVeto {
    /** @return the reason shown to the player, or null to allow */
    @Nullable
    Component check(ServerPlayer player, Target fob);

    record Target(UUID factionId, ResourceKey<Level> dim, BlockPos pos, String name) {}
}
