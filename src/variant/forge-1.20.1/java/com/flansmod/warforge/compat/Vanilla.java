package com.flansmod.warforge.compat;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Vanilla objects whose field names or types differ between versions. */
public final class Vanilla {
    private Vanilla() {}

    public static SoundEvent explodeSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    /** The short grass plant (called "grass" before 1.20.3). */
    public static Block shortGrass() {
        return Blocks.GRASS;
    }
}
