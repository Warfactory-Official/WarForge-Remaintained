package com.flansmod.warforge.platform;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Arrays;

/** Extractions in a transaction are recorded, applied on outermost commit, dropped on abort. Insertion always refused. */
public final class ExtractOnlyItemResources extends SnapshotJournal<int[]> implements ResourceHandler<ItemResource> {
    private final IItemHandler handler;
    private int[] pending = new int[0];

    public ExtractOnlyItemResources(IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public int size() {
        return handler.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        ItemStack stack = handler.getStackInSlot(index);
        return stack.getCount() - pending(index) <= 0 ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        return Math.max(0, handler.getStackInSlot(index).getCount() - pending(index));
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return handler.getSlotLimit(index);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return false;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ItemStack stack = handler.getStackInSlot(index);
        if (stack.isEmpty() || !resource.matches(stack)) {
            return 0;
        }
        int available = handler.extractItem(index, stack.getCount(), true).getCount() - pending(index);
        int extracted = Math.min(amount, available);
        if (extracted <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        ensureSize(index + 1);
        pending[index] += extracted;
        return extracted;
    }

    @Override
    protected int[] createSnapshot() {
        return pending.clone();
    }

    @Override
    protected void revertToSnapshot(int[] snapshot) {
        pending = snapshot;
    }

    @Override
    protected void onRootCommit(int[] originalState) {
        int[] toApply = pending;
        pending = new int[0];
        for (int slot = 0; slot < toApply.length; slot++) {
            if (toApply[slot] > 0) {
                handler.extractItem(slot, toApply[slot], false);
            }
        }
    }

    private int pending(int index) {
        return index < pending.length ? pending[index] : 0;
    }

    private void ensureSize(int size) {
        if (pending.length < size) {
            pending = Arrays.copyOf(pending, size);
        }
    }
}
