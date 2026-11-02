package com.flansmod.warforge.platform;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public abstract class WFMobEffect extends MobEffect {
    protected WFMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    protected boolean onEffectTick(LivingEntity entity, int amplifier) {
        return true;
    }

    protected boolean shouldTick(int remainingDuration, int amplifier) {
        return false;
    }

    @Override
    public final void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!onEffectTick(entity, amplifier)) entity.removeEffect(this);
    }

    @Override
    public final boolean isDurationEffectTick(int duration, int amplifier) {
        return shouldTick(duration, amplifier);
    }
}
