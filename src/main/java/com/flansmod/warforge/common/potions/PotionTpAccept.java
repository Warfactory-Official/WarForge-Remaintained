package com.flansmod.warforge.common.potions;

import com.flansmod.warforge.platform.WFMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class PotionTpAccept extends WFMobEffect
{
	protected PotionTpAccept()
	{
		super(MobEffectCategory.NEUTRAL, 0x00ffff);
	}

	@Override
	protected boolean onEffectTick(LivingEntity living, int amplifier)
	{
		return true;
	}

	@Override
	protected boolean shouldTick(int duration, int amplifier)
	{
		return duration % 20 == 0;
	}
}
