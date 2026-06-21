package com.flansmod.warforge.server;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class TeleportUtil {

    public static void teleportPlayer(ServerPlayer player, ResourceKey<Level> targetDimension, BlockPos targetPosition) {
        // Ensure the target dimension is different from the current dimension
        if (player.level().dimension() != targetDimension) {
            ServerLevel targetWorld = player.server.getLevel(targetDimension);

            // Change the player's dimension and teleport them
            player.changeDimension(targetWorld, new WfTeleporter());
        }

        // Set the player's location in the target dimension
        player.connection.teleport(
                targetPosition.getX() + 0.5D,
                targetPosition.getY() + 1.5D,
                targetPosition.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
    }
}
