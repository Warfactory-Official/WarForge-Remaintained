package com.flansmod.warforge.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class ItemHandlers {
    private ItemHandlers() {}

    public static CompoundTag serialize(ItemStackHandler handler, HolderLookup.Provider registries) {
        return handler.serializeNBT();
    }

    public static Object capabilityView(IItemHandler pullOnlyView) {
        return pullOnlyView;
    }
}
