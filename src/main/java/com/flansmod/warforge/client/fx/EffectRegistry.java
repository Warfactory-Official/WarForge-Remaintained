package com.flansmod.warforge.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Random;

public final class EffectRegistry {
    private static final Map<String, IEffect> EFFECTS = Map.of(
            "upgrade", new EffectUpgrade(),
            "disband", new EffectDisband());

    private EffectRegistry() {}

    public static void runClientEffect(String type, Player player, double x, double y, double z, CompoundTag data) {
        IEffect effect = EFFECTS.get(type);
        if (effect == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        effect.runEffect(mc.level, player, mc.getTextureManager(), new Random(), x, y, z, data);
    }
}
