package com.flansmod.warforge.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class WFBlockEntity extends BlockEntity {
    protected WFBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void writeSyncData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void readSyncData(CompoundTag tag, HolderLookup.Provider registries) {}

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
