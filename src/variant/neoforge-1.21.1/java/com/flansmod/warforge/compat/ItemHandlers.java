package com.flansmod.warforge.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Item handler save format ({"Items": [{Slot, item...}], "Size": n}). The handler API differs between versions. */
public final class ItemHandlers {
    private ItemHandlers() {}

    public static CompoundTag serialize(ItemStackHandler handler, HolderLookup.Provider registries) {
        return handler.serializeNBT(registries);
    }
}
