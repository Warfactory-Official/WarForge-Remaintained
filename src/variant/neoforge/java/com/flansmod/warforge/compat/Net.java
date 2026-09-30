package com.flansmod.warforge.compat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** Packet buffer helpers whose vanilla API differs between versions. */
public final class Net {
    private Net() {}

    /** Writes an item stack (empty stacks allowed). The buffer is the one a packet's encodeInto receives. */
    public static void writeItem(FriendlyByteBuf buf, ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack);
    }

    public static ItemStack readItem(FriendlyByteBuf buf) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
    }
}
