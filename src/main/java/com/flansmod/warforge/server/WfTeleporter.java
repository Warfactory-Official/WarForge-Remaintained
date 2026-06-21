package com.flansmod.warforge.server;

import java.util.function.Function;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.ITeleporter;

public class WfTeleporter implements ITeleporter {

    @Override
    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
        // No portal handling; just reposition the entity into the destination dimension at the default spot.
        return repositionEntity.apply(false);
    }
}
