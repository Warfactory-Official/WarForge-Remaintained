package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.compat.ItemHandlers;

import com.flansmod.warforge.api.WarForgeCapabilities;
import net.minecraft.world.Containers;

import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.PullOnlyItemHandler;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Set;
import com.flansmod.warforge.compat.Nbt;
import com.flansmod.warforge.compat.Stacks;

public class IslandCollectorBlockEntity extends YieldCollectorBlockEntity {
    /** Slot count for the faction yield collector, configurable via {@link WarForgeConfig#ISLAND_COLLECTOR_SLOTS}. */
    private static int slotCount() {
        return Math.max(1, WarForgeConfig.ISLAND_COLLECTOR_SLOTS);
    }

    private final ItemStackHandler storage = new ItemStackHandler(slotCount()) {
        @Override
        protected void onContentsChanged(int slot) {
            IslandCollectorBlockEntity.this.setChanged();
        }
    };

    private final IItemHandler pullOnlyView = new PullOnlyItemHandler(storage);

    public IItemHandler getPullOnlyView() {
        return pullOnlyView;
    }

    private final Object itemCapability = ItemHandlers.capabilityView(pullOnlyView);

    public Object getItemCapability() {
        return itemCapability;
    }

    public IslandCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(Content.TE_ISLAND_COLLECTOR.get(), pos, state);
    }

    @Override
    public int getDefenceStrength() {
        return 0;
    }

    @Override
    public int getSupportStrength() {
        return 0;
    }

    @Override
    public int getAttackStrength() {
        return 0;
    }

    @Override
    public boolean canBeSieged() {
        return false;
    }

    @Override
    protected float getYieldMultiplier() {
        return 1.0f;
    }

    @Override
    public String getClaimDisplayName() {
        return factionName.isEmpty() ? "Faction Yield Storage" : factionName + " Yield Storage";
    }

    @Override
    public String getName() {
        return getClaimDisplayName();
    }

    public IItemHandlerModifiable getStorageHandler() {
        return storage;
    }

    public void processIslandYields(Faction faction) {
        if (level == null || level.isClientSide() || faction == null) {
            return;
        }

        DimChunkPos collectorChunk = new DimChunkPos(level.dimension(), worldPosition);
        Set<DimChunkPos> island = WarForgeMod.FACTIONS.claims.collectIsland(faction.getUuid(), collectorChunk);
        if (island.isEmpty()) {
            return;
        }

        for (DimBlockPos claimPos : faction.getClaimPositions()) {
            if (island.contains(claimPos.toChunkPos())) {
                processYieldForClaim(faction, claimPos);
            }
        }
    }

    @Override
    public void onLoad() {
        if (level == null || level.isClientSide()) {
            return;
        }

        Faction faction = WarForgeMod.FACTIONS.registry.getFaction(factionUUID);
        if (faction != null) {
            processIslandYields(faction);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        WarForgeCapabilities.invalidate(this);
    }

    // insertion refused (isItemValid)
    @Override
    public int getSlots() {
        return storage.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < storage.getSlots(); i++) {
            if (!storage.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return (index >= 0 && index < storage.getSlots()) ? storage.getStackInSlot(index) : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int index, ItemStack stack) {
        if (index >= 0 && index < storage.getSlots()) {
            storage.setStackInSlot(index, stack);
        }
    }

    @Override
    public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
        return storage.insertItem(index, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int index, int count, boolean simulate) {
        return storage.extractItem(index, count, simulate);
    }

    @Override
    public int getSlotLimit(int index) {
        return storage.getSlotLimit(index);
    }

    @Override
    public boolean isItemValid(int index, ItemStack stack) {
        return false;
    }

    @Override
    public void clear() {
        for (int i = 0; i < storage.getSlots(); i++) {
            storage.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void onBlockRemoved() {
        for (int i = 0; i < getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getStackInSlot(i));
        }
    }

    @Override
    protected void writeData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeData(nbt, registries);
        nbt.put("storage", ItemHandlers.serialize(storage, registries));
    }

    @Override
    protected void readData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.readData(nbt, registries);

        // manual load: configured slot count wins; items from vanished slots relocate to free space
        if (nbt.contains("storage")) {
            ListTag items = Nbt.getList(Nbt.getCompound(nbt, "storage"), "Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTags = Nbt.getCompound(items, i);
                int slot = Nbt.getInt(itemTags, "Slot");
                ItemStack stack = Stacks.parse(registries, itemTags);
                if (stack.isEmpty()) {
                    continue;
                }
                if (slot >= 0 && slot < storage.getSlots()) {
                    storage.setStackInSlot(slot, stack);
                } else {
                    ItemHandlerHelper.insertItem(storage, stack, false);
                }
            }
        }
    }
}
