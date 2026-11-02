package com.flansmod.warforge.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Teleports {
    private Teleports() {}

    public static @Nullable Entity changeDimension(Entity entity, ServerLevel level, Vec3 pos) {
        return entity.changeDimension(new DimensionTransition(level, pos, entity.getDeltaMovement(),
                entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
    }

    public static void moveTo(Entity entity, double x, double y, double z, float yRot, float xRot) {
        entity.moveTo(x, y, z, yRot, xRot);
    }

    public static boolean startRiding(Entity entity, Entity vehicle, boolean force) {
        return entity.startRiding(vehicle, force);
    }
}
