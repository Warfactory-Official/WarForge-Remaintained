package com.flansmod.warforge.platform;

import net.minecraft.core.Holder;

import java.util.function.Supplier;

/** A registered object (made by WFRegistry). {@link #get()} works once registration has run. */
public interface RegHandle<T> extends Supplier<T> {
    @Override
    T get();

    /** The registry holder of the object (a Holder of the registry's type). */
    Holder<?> holder();
}
