package com.flansmod.warforge.platform;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** A living entity is about to take damage (before armour and invulnerability). Cancel to prevent it. */
public interface LivingDamage {
    LivingEntity getEntity();

    DamageSource getSource();

    void setCanceled(boolean canceled);
}
