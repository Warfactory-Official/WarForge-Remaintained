package com.flansmod.warforge.client.fx;

import com.flansmod.warforge.compat.Vanilla;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Random;

public class EffectDisband implements IEffect {
    @OnlyIn(Dist.CLIENT)
    @Override
    public void runEffect(Level world, Player player, TextureManager man, Random rand, double x, double y, double z, CompoundTag data) {

        world.playLocalSound(x, y, z, Vanilla.explodeSound(), SoundSource.PLAYERS, 1.0F, 0.9F, false);
        world.playLocalSound(x, y, z, SoundEvents.ANVIL_DESTROY, SoundSource.PLAYERS, 1.0F, 0.9F, false);

        for (int i = 0; i < 32; i++) {
            world.addParticle(
                    ParticleTypes.EXPLOSION_EMITTER,
                    x + world.getRandom().nextFloat(),
                    y + (i % 2) + world.getRandom().nextFloat(),
                    z + world.getRandom().nextFloat(),
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }
}
