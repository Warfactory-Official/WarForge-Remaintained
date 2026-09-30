package com.flansmod.warforge.common.potions;

import java.util.List;

import com.flansmod.warforge.common.WarForgeMod;

import com.flansmod.warforge.platform.WFMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import com.flansmod.warforge.compat.Effects;

public class PotionTpRequest extends WFMobEffect
{
	protected PotionTpRequest()
	{
		super(MobEffectCategory.NEUTRAL, 0x00afff);
	}

	@Override
	protected boolean onEffectTick(LivingEntity living, int amplifier)
	{
		LivingEntity bestEntity = null;
		double bestDistanceSq = Double.MAX_VALUE;

		AABB searchBox = living.getBoundingBox().inflate(64.0D);
		List<LivingEntity> candidates = living.level().getEntitiesOfClass(LivingEntity.class, searchBox);
		for (LivingEntity entity : candidates)
		{
			if (Effects.has(entity, WarForgeMod.POTIONS.tpAccept))
			{
				double distanceSq = entity.distanceToSqr(living);
				if (distanceSq < bestDistanceSq)
				{
					bestEntity = entity;
					bestDistanceSq = distanceSq;
				}
			}
		}

		if (bestEntity != null)
		{
			if (living.randomTeleport(bestEntity.getX(), bestEntity.getY(), bestEntity.getZ(), true))
			{
				Effects.remove(living, WarForgeMod.POTIONS.tpRequest);
			}
		}
		return true;
	}

	@Override
	protected boolean shouldTick(int duration, int amplifier)
	{
		return duration % 20 == 0;
	}
}
