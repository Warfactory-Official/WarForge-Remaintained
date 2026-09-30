package com.flansmod.warforge.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Moving entities between dimensions and positions. The vanilla API differs between versions. */
public final class Teleports {
    private Teleports() {}

    /**
     * Moves an entity to another dimension at pos, keeping its velocity and rotation. Returns the entity in the new
     * dimension (may be a new object), or null if it could not be moved.
     */
    public static @Nullable Entity changeDimension(Entity entity, ServerLevel level, Vec3 pos) {
        return entity.changeDimension(new DimensionTransition(level, pos, entity.getDeltaMovement(),
                entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
    }

    /** Sets position and rotation (no interpolation). */
    public static void moveTo(Entity entity, double x, double y, double z, float yRot, float xRot) {
        entity.moveTo(x, y, z, yRot, xRot);
    }

    /** Mounts entity on vehicle (force: even if the vehicle normally refuses). */
    public static boolean startRiding(Entity entity, Entity vehicle, boolean force) {
        return entity.startRiding(vehicle, force);
    }
}
