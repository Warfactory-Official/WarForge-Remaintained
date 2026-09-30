package com.flansmod.warforge.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Base class of WarForge mob effects. The vanilla tick methods differ between versions. */
public abstract class WFMobEffect extends MobEffect {
    protected WFMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /** Runs on the ticks where {@link #shouldTick} is true. Returns false to remove the effect. */
    protected boolean onEffectTick(LivingEntity entity, int amplifier) {
        return true;
    }

    /** True on the ticks where {@link #onEffectTick} runs. */
    protected boolean shouldTick(int remainingDuration, int amplifier) {
        return false;
    }

    @Override
    public final boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        return onEffectTick(entity, amplifier);
    }

    @Override
    public final boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return shouldTick(duration, amplifier);
    }
}
