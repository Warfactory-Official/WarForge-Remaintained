package com.flansmod.warforge.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Item handler save format ({"Items": [{Slot, item...}], "Size": n}). The handler API differs between versions. */
public final class ItemHandlers {
    private ItemHandlers() {}

    /** Writes the same compound as ItemStackHandler.serializeNBT did before it was removed. */
    public static CompoundTag serialize(ItemStackHandler handler, HolderLookup.Provider registries) {
        ListTag items = new ListTag();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = Stacks.save(stack, registries);
                itemTag.putInt("Slot", i);
                items.add(itemTag);
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items);
        tag.putInt("Size", handler.getSlots());
        return tag;
    }
}
