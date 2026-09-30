package com.flansmod.warforge.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class ParticleStarCircle extends TextureSheetParticle {

    private ParticleStarCircle(ClientLevel level, double x, double y, double z, float r, float g, float b) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.lifetime = 20;
        this.friction = 0.96f;
        this.hasPhysics = false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public TextureSheetParticle createParticle(
                SimpleParticleType type, ClientLevel level,
                double x, double y, double z,
                double dx, double dy, double dz) {
            ParticleStarCircle p = new ParticleStarCircle(level, x, y, z, (float) dx, (float) dy, (float) dz);
            p.pickSprite(sprites);
            return p;
        }
    }
}
