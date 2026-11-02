package com.flansmod.warforge.common;

import com.flansmod.warforge.common.network.PacketEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Server side of the particle effects played by client/fx. */
public final class EffectPackets {
    private EffectPackets() {}

    public static void disband(ResourceKey<Level> dim, BlockPos pos, float radius) {
        send(dim, pos, radius, PacketEffect.TYPE_DISBAND, new CompoundTag());
    }

    public static void upgrade(ResourceKey<Level> dim, BlockPos pos, float radius, int segments, double circleRadius, double speed, int color, int count) {
        CompoundTag data = new CompoundTag();
        data.putInt("segments", segments);
        data.putDouble("radius", circleRadius);
        data.putDouble("speed", speed);
        data.putInt("color", color);
        data.putInt("count", count);
        send(dim, pos, radius, PacketEffect.TYPE_UPGRADE, data);
    }

    private static void send(ResourceKey<Level> dim, BlockPos pos, float radius, String type, CompoundTag data) {
        PacketEffect packet = new PacketEffect();
        packet.x = pos.getX();
        packet.y = pos.getY();
        packet.z = pos.getZ();
        packet.type = type;
        packet.dataNBT = data.toString();
        WarForgeMod.NETWORK.sendToAllAround(packet, pos.getX(), pos.getY(), pos.getZ(), radius, dim);
    }
}
