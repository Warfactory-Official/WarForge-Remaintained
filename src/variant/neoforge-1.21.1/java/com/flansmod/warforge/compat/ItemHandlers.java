package com.flansmod.warforge.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class ItemHandlers {
    private ItemHandlers() {}

    public static CompoundTag serialize(ItemStackHandler handler, HolderLookup.Provider registries) {
        return handler.serializeNBT(registries);
    }

    public static Object capabilityView(IItemHandler pullOnlyView) {
        return pullOnlyView;
    }
}
