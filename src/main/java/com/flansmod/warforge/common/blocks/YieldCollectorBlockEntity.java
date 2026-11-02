package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.faction.FactionIds;

import org.apache.commons.lang3.tuple.Pair;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.VeinCodec;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.InventoryHelper;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.common.item.ItemMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.Arrays;

import static com.flansmod.warforge.common.WarForgeMod.VEIN_HANDLER;
import com.flansmod.warforge.compat.Nbt;
import com.flansmod.warforge.compat.Stacks;

public abstract class YieldCollectorBlockEntity extends ClaimBlockEntity implements IItemHandlerModifiable {
    public static final int NUM_YIELD_STACKS = 9;
    public static final int NUM_BASE_SLOTS = NUM_YIELD_STACKS;

    protected abstract float getYieldMultiplier();

    protected ItemStack[] yieldStacks = new ItemStack[NUM_YIELD_STACKS];

    public YieldCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Arrays.fill(yieldStacks, ItemStack.EMPTY);
    }

    public void processYield(Faction faction) {
        processYieldForClaim(faction, getClaimPos());
    }

    protected void processYieldForClaim(Faction faction, DimBlockPos claimPos) {
        processYieldForClaim(faction, claimPos, true);
    }

    protected void processYieldForClaim(Faction faction, DimBlockPos claimPos, boolean warnOnFull) {
        if(level.isClientSide()) { return; }
        if (VEIN_HANDLER == null || !VEIN_HANDLER.hasFinishedInit) { return; }

        int numYields = faction.takePendingYields(claimPos);
        if (numYields == 0) { return; }

        DimChunkPos currPos = claimPos.toChunkPos();
        if (!VEIN_HANDLER.dimHasVeins(currPos.dim)) { return; }

        long seed = ((ServerLevel) level).getSeed();

        Pair<Vein, Quality> veinInfo = VEIN_HANDLER.getVein(level.dimension(), currPos.x, currPos.z, seed);
        if (veinInfo == null) { return; }

        Vein currVein = veinInfo.getLeft();

        RandomSource rand = level.getRandom();
        ArrayList<ItemStack> yieldComps = new ArrayList<>(currVein.compIds.size());
        float yieldMult = getYieldMultiplier();

        for (ItemMatcher currComp : currVein.compIds) {
            int numItems = 0;
            ArrayList<short[]> subCompYieldInfos = VeinCodec.getYieldInfo(currComp, veinInfo, currPos.dim);

            for (int j = 0; j < numYields; ++j) {
                for (short[] subCompInfo : subCompYieldInfos) {
                    if (rand.nextInt(VeinCodec.WEIGHT_FRACTION_TENS_POW) + 1 > subCompInfo[0]) { continue; }

                    numItems += subCompInfo[1];
                    if (rand.nextInt(VeinCodec.WEIGHT_FRACTION_TENS_POW) + 1 > subCompInfo[2]) { continue; }

                    ++numItems;
                }
            }

            numItems = Math.round(numItems * yieldMult);

            if (numItems == 0) { continue; }  // 0-count stack would be air

            ItemStack compStack = currComp.toStack(numItems);
            if (compStack.isEmpty()) {
                WarForgeMod.LOGGER.warn("Vein {} has an item component that does not resolve", currVein.translationKey);
                continue;
            }

            yieldComps.add(compStack);
        }

        if (yieldComps.isEmpty()) { return; }

        // addItemStackToInventory consumes the stack
        for (ItemStack currCompStack : yieldComps) {
            if(!InventoryHelper.addItemStackToInventory(this, currCompStack, false) && warnOnFull) {
                WarForgeMod.LOGGER.debug("Yield collector at {} is full; dropped {}", getBlockPos(), currCompStack);
            }
        }

        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(!level.isClientSide()) {
            Faction faction = WarForgeMod.FACTIONS.registry.getFaction(factionUUID);
            if(faction != null) {
                processYield(faction);
            } else if(!factionUUID.equals(FactionIds.NONE)) {
                WarForgeMod.LOGGER.warn("Loaded yield collector at {} with unknown faction {}", worldPosition, factionUUID);
            }
        }
    }

    @Override
    protected void writeData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeData(nbt, registries);

        for(int i = 0; i < NUM_YIELD_STACKS; i++) {
            nbt.put("yield_" + i, Stacks.save(yieldStacks[i], registries));
        }
    }

    @Override
    protected void readData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.readData(nbt, registries);

        for(int i = 0; i < NUM_YIELD_STACKS; i++) {
            if(nbt.contains("yield_" + i))
                yieldStacks[i] = Stacks.parse(registries, Nbt.getCompound(nbt, "yield_" + i));
            else
                yieldStacks[i] = ItemStack.EMPTY;
        }
    }

    public String getName() {
        if (factionName == null || factionName.isEmpty()) {
            return "WarForge Storage";
        }
        return factionName;
    }

    // yield stacks in slots 0-8
    @Override
    public int getSlots() { return NUM_BASE_SLOTS; }

    @Override
    public ItemStack getStackInSlot(int index) {
        if(index < NUM_YIELD_STACKS)
            return yieldStacks[index];
        return ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int index, ItemStack stack) {
        if(index < NUM_YIELD_STACKS) {
            yieldStacks[index] = stack;
            setChanged();
        }
    }

    @Override
    public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
        if(stack.isEmpty() || index < 0 || index >= NUM_YIELD_STACKS || !isItemValid(index, stack)) {
            return stack;
        }

        ItemStack existing = yieldStacks[index];
        int limit = Math.min(getSlotLimit(index), stack.getMaxStackSize());

        if(!existing.isEmpty()) {
            if(!Stacks.sameItemSameData(existing, stack)) {
                return stack;
            }
            limit -= existing.getCount();
        }

        if(limit <= 0) {
            return stack;
        }

        int toInsert = Math.min(limit, stack.getCount());
        if(!simulate) {
            if(existing.isEmpty()) {
                ItemStack inserted = stack.copy();
                inserted.setCount(toInsert);
                yieldStacks[index] = inserted;
            } else {
                existing.grow(toInsert);
            }
            setChanged();
        }

        if(stack.getCount() == toInsert) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = stack.copy();
        remainder.shrink(toInsert);
        return remainder;
    }

    @Override
    public ItemStack extractItem(int index, int count, boolean simulate) {
        if(count <= 0 || index < 0 || index >= NUM_YIELD_STACKS) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = yieldStacks[index];
        if(existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(count, existing.getCount());
        ItemStack result = existing.copy();
        result.setCount(toExtract);
        if(!simulate) {
            if(toExtract >= existing.getCount()) {
                yieldStacks[index] = ItemStack.EMPTY;
            } else {
                existing.shrink(toExtract);
            }
            setChanged();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int index) {
        return 64;
    }

    @Override
    public boolean isItemValid(int index, ItemStack stack) {
        return index < NUM_YIELD_STACKS;
    }

    public boolean isEmpty() {
        for(int i = 0; i < NUM_YIELD_STACKS; i++)
            if(!yieldStacks[i].isEmpty())
                return false;
        return true;
    }

    public void clear() {
        Arrays.fill(yieldStacks, ItemStack.EMPTY);
    }
}
