package com.flansmod.warforge.common.effect;

import net.neoforged.bus.api.EventPriority;

import java.util.ArrayList;
import java.util.List;

public class AnimatedEffectHandler {
    public static final List<EffectAnimated<?>> effectQueue = new ArrayList<>();

    public static void add(EffectAnimated<?> effect) {
        effectQueue.add(effect);
    }

    public void onClientTick() {
        effectQueue.removeIf(effect -> {
            effect.tick();
            return effect.isComplete();
        });
    }

}
