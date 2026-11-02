package com.flansmod.warforge.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class Teleports {
    private Teleports() {}

    public static @Nullable Entity changeDimension(Entity entity, ServerLevel level, Vec3 pos) {
        return entity.changeDimension(level, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity e, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(pos, e.getDeltaMovement(), e.getYRot(), e.getXRot());
            }

            @Override
            public boolean playTeleportSound(net.minecraft.server.level.ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
                return false;
            }
        });
    }

    public static void moveTo(Entity entity, double x, double y, double z, float yRot, float xRot) {
        entity.moveTo(x, y, z, yRot, xRot);
    }

    public static boolean startRiding(Entity entity, Entity vehicle, boolean force) {
        return entity.startRiding(vehicle, force);
    }
}
