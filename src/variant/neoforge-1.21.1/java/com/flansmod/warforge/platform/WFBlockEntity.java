package com.flansmod.warforge.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base class of every WarForge block entity. The vanilla save/load and client-sync methods differ between
 * versions; this class maps them to the four hooks below, which always work on a CompoundTag with the same keys.
 */
public abstract class WFBlockEntity extends BlockEntity {
    protected WFBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Writes the saved data. Call super first. */
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Reads the saved data. Call super first. */
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Writes the data sent to clients (chunk load and block updates). Call super first. */
    protected void writeSyncData(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Reads the data sent to clients. */
    protected void readSyncData(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Server side: the block of this block entity is being replaced by a different block (before removal). */
    public void onBlockRemoved() {}

    @Override
    protected final void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeData(tag, registries);
    }

    @Override
    protected final void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readData(tag, registries);
    }

    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeSyncData(tag, registries);
        return tag;
    }

    @Override
    public final void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readSyncData(tag, registries);
    }

    @Override
    public final void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readSyncData(packet.getTag(), registries);
    }
}
