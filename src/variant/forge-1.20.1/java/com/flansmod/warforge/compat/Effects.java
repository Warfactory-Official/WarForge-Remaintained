package com.flansmod.warforge.compat;

import com.flansmod.warforge.platform.RegHandle;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Mob effect helpers. From 1.20.5 effects are passed as registry holders, before that as the effect object. */
public final class Effects {
    private Effects() {}

    public static MobEffectInstance instance(RegHandle<MobEffect> effect, int durationTicks) {
        return new MobEffectInstance(effect.get(), durationTicks);
    }

    public static boolean has(LivingEntity entity, RegHandle<MobEffect> effect) {
        return entity.hasEffect(effect.get());
    }

    public static boolean remove(LivingEntity entity, RegHandle<MobEffect> effect) {
        return entity.removeEffect(effect.get());
    }
}
