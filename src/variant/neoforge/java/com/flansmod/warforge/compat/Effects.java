package com.flansmod.warforge.compat;

import com.flansmod.warforge.platform.RegHandle;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Mob effect helpers. From 1.20.5 effects are passed as registry holders, before that as the effect object. */
public final class Effects {
    private Effects() {}

    @SuppressWarnings("unchecked")
    private static Holder<MobEffect> holder(RegHandle<MobEffect> effect) {
        return (Holder<MobEffect>) effect.holder();
    }

    public static MobEffectInstance instance(RegHandle<MobEffect> effect, int durationTicks) {
        return new MobEffectInstance(holder(effect), durationTicks);
    }

    public static boolean has(LivingEntity entity, RegHandle<MobEffect> effect) {
        return entity.hasEffect(holder(effect));
    }

    public static boolean remove(LivingEntity entity, RegHandle<MobEffect> effect) {
        return entity.removeEffect(holder(effect));
    }
}
