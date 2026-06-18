package com.flansmod.warforge.common.blocks;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.flansmod.warforge.client.GuiIslandCollector;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.PullOnlyItemHandler;
import com.flansmod.warforge.server.Faction;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public class TileEntityIslandCollector extends TileEntityYieldCollector implements IGuiHolder<PosGuiData> {
    /** Slot count for the faction yield collector, configurable via {@link WarForgeConfig#ISLAND_COLLECTOR_SLOTS}. */
    private static int slotCount() {
        return Math.max(1, WarForgeConfig.ISLAND_COLLECTOR_SLOTS);
    }

    // The real backing store. Yields are deposited here by the mod; the GUI binds directly to it.
    private final ItemStackHandler storage = new ItemStackHandler(slotCount()) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEntityIslandCollector.this.markDirty();
        }
    };

    // The view exposed to automation: pull-only, available from every side.
    private final IItemHandler pullOnlyView = new PullOnlyItemHandler(storage);

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

    @Override
    public boolean hasCustomName() {
        return true;
    }

    public IItemHandlerModifiable getStorageHandler() {
        return storage;
    }

    public void processIslandYields(Faction faction) {
        if (world == null || world.isRemote || faction == null) {
            return;
        }

        DimChunkPos collectorChunk = new DimChunkPos(world.provider.getDimension(), pos);
        Set<DimChunkPos> island = WarForgeMod.FACTIONS.collectFactionIsland(faction.uuid, collectorChunk);
        if (island.isEmpty()) {
            return;
        }

        for (DimBlockPos claimPos : faction.claims.keySet()) {
            if (island.contains(claimPos.toChunkPos())) {
                processYieldForClaim(faction.claims, claimPos);
            }
        }
    }

    @Override
    public void onLoad() {
        if (world == null || world.isRemote) {
            return;
        }

        Faction faction = WarForgeMod.FACTIONS.getFaction(factionUUID);
        if (faction != null) {
            processIslandYields(faction);
        }
    }

    // ----------------------------------------------------------
    // Capability: expose the storage as a pull-only item handler on every side.
    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(pullOnlyView);
        }
        return super.getCapability(capability, facing);
    }

    // ----------------------------------------------------------
    // IInventory delegates to the 100-slot storage so yield deposits, break-drops and the GUI all
    // share one backing store. Insertion is refused (isItemValidForSlot) so nothing can be put in.
    @Override
    public int getSizeInventory() {
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
    public ItemStack decrStackSize(int index, int count) {
        return (index >= 0 && index < storage.getSlots()) ? storage.extractItem(index, count, false) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (index < 0 || index >= storage.getSlots()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = storage.getStackInSlot(index);
        storage.setStackInSlot(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index >= 0 && index < storage.getSlots()) {
            storage.setStackInSlot(index, stack);
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return false;
    }

    @Override
    public void clear() {
        for (int i = 0; i < storage.getSlots(); i++) {
            storage.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ----------------------------------------------------------
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("storage", storage.serializeNBT());
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        // Load the saved items into the (config-sized) handler manually, rather than via
        // deserializeNBT, so that the configured slot count always wins. Items whose saved slot no
        // longer exists are relocated into any remaining free space instead of being dropped.
        if (nbt.hasKey("storage")) {
            NBTTagList items = nbt.getCompoundTag("storage").getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < items.tagCount(); i++) {
                NBTTagCompound itemTags = items.getCompoundTagAt(i);
                int slot = itemTags.getInteger("Slot");
                ItemStack stack = new ItemStack(itemTags);
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

        // Migrate any items saved by the old 9-slot layout into the new storage.
        for (int i = 0; i < NUM_YIELD_STACKS; i++) {
            ItemStack legacy = yieldStacks[i];
            if (legacy != null && !legacy.isEmpty()) {
                ItemHandlerHelper.insertItem(storage, legacy, false);
                yieldStacks[i] = ItemStack.EMPTY;
            }
        }
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
        return GuiIslandCollector.buildUI(guiData, syncManager, settings, this);
    }
}
