package com.flansmod.warforge.server.fob;

import java.util.Map;
import java.util.HashMap;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.compat.Teleports;
import com.flansmod.warforge.server.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

/** Stand-still countdown before a FOB warp. Holds ids, never entities: players can log out mid-warp. */
public class FobWarpQueue {
    private static final int TICKS_PER_SECOND = 20;
    private static final double LANDING_OFFSET_Y = 1.5D;
    private final List<PendingWarp> pendingWarps = new ArrayList<>();

    public static void forceTouchChunk(DimBlockPos target) {
        ServerLevel level = WarForgeMod.MC_SERVER.getLevel(target.dim);
        if (level != null) {
            level.getChunk(target.getX() >> 4, target.getZ() >> 4);
        }
    }

    private static volatile Map<String, Integer> vehicleCost = Map.of();

    /** Parses the configured {@code entityTypeId=extraCost} entries. */
    public static void configureVehicleCost(List<String> entries) {
        HashMap<String, Integer> parsed = new HashMap<>();
        for (String entry : entries) {
            String[] split = entry.split("=");
            if (split.length != 2) {
                WarForgeMod.LOGGER.warn("Ignoring malformed FOB vehicle cost '{}'", entry);
                continue;
            }
            try {
                parsed.put(split[0].trim(), Integer.parseInt(split[1].trim()));
            } catch (NumberFormatException e) {
                WarForgeMod.LOGGER.warn("Ignoring FOB vehicle cost '{}'", entry, e);
            }
        }
        vehicleCost = Map.copyOf(parsed);
    }

    public static int vehicleExtraCost(Entity vehicle) {
        if (vehicle == null) {
            return 0;
        }
        Integer extra = vehicleCost.get(BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString());
        return extra == null ? 0 : Math.max(0, extra);
    }

    private static void warpVehicle(ServerPlayer player, UUID vehicleId, DimBlockPos target) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !vehicle.getUUID().equals(vehicleId)) {
            return;
        }
        ServerLevel targetLevel = WarForgeMod.MC_SERVER.getLevel(target.dim);
        if (targetLevel == null) {
            return;
        }

        List<Entity> passengers = new ArrayList<>(vehicle.getPassengers());
        vehicle.ejectPassengers();

        double x = target.getX() + 0.5D;
        double y = target.getY() + LANDING_OFFSET_Y;
        double z = target.getZ() + 0.5D;

        Entity movedVehicle = vehicle;
        if (!movedVehicle.level().dimension().equals(target.dim)) {
            movedVehicle = Teleports.changeDimension(movedVehicle, targetLevel, new Vec3(x, y, z));
        }
        if (movedVehicle == null) {
            return;
        }
        Teleports.moveTo(movedVehicle, x, y, z, movedVehicle.getYRot(), movedVehicle.getXRot());

        for (Entity passenger : passengers) {
            if (passenger == player) {
                continue;
            }
            Entity moved = passenger;
            if (!moved.level().dimension().equals(target.dim)) {
                moved = Teleports.changeDimension(moved, targetLevel, new Vec3(x, y, z));
            }
            if (moved != null) {
                Teleports.moveTo(moved, x, y, z, moved.getYRot(), moved.getXRot());
                Teleports.startRiding(moved, movedVehicle, true);
            }
        }
    }

    public boolean hasPendingWarp(UUID playerId) {
        for (PendingWarp warp : pendingWarps) {
            if (warp.playerId.equals(playerId)) return true;
        }
        return false;
    }

    public void requestFobWarp(ServerPlayer player, Fob fob, int cost) {
        PendingWarp warp = new PendingWarp();
        warp.playerId = player.getUUID();
        warp.vehicleId = player.getVehicle() == null ? null : player.getVehicle().getUUID();
        warp.pos = player.blockPosition();
        warp.target = new DimBlockPos(fob.pos.dim, fob.pos.getX(), fob.pos.getY(), fob.pos.getZ());
        warp.fob = fob;
        warp.cost = cost;
        warp.ticksRemaining = WarForgeConfig.FOB_WARP_TICKS;
        pendingWarps.add(warp);
        player.sendSystemMessage(t("fob.warp_started"));
    }

    /** Logout: the warp is dropped and its tickets refunded. */
    public void cancel(UUID playerId) {
        for (int i = pendingWarps.size() - 1; i >= 0; i--) {
            PendingWarp warp = pendingWarps.get(i);
            if (warp.playerId.equals(playerId)) {
                refund(warp);
                pendingWarps.remove(i);
            }
        }
    }

    public void update() {
        for (int i = pendingWarps.size() - 1; i >= 0; i--) {
            PendingWarp warp = pendingWarps.get(i);
            ServerPlayer player = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(warp.playerId);

            if (player == null || !player.blockPosition().equals(warp.pos)) {
                refund(warp);
                if (player != null) {
                    player.sendSystemMessage(t("fob.warp_cancelled"));
                }
                pendingWarps.remove(i);
                continue;
            }
            if (warp.ticksRemaining % TICKS_PER_SECOND == 0) {
                player.sendSystemMessage(t("fob.warp_countdown", warp.ticksRemaining / TICKS_PER_SECOND));
            }
            if (--warp.ticksRemaining <= 0) {
                forceTouchChunk(warp.target);
                warpVehicle(player, warp.vehicleId, warp.target);
                TeleportUtil.teleportPlayer(player, warp.target.dim, warp.target.toRegularPos());
                pendingWarps.remove(i);
            }
        }
    }

    private static void refund(PendingWarp warp) {
        warp.fob.tickets = Math.min(warp.fob.maxTickets, warp.fob.tickets + warp.cost);
    }

    private static final class PendingWarp {
        private UUID playerId;
        private UUID vehicleId;
        private int ticksRemaining;
        private BlockPos pos;
        private DimBlockPos target;
        private Fob fob;
        private int cost;
    }
}
