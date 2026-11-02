package com.flansmod.warforge.platform;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** From 1.21.6 vanilla saves via ValueInput/ValueOutput; hooks keep a CompoundTag whose top-level entries are copied through unchanged. */
public abstract class WFBlockEntity extends BlockEntity {
    @SuppressWarnings("deprecation")
    private static final MapCodec<CompoundTag> WHOLE_TAG = MapCodec.assumeMapUnsafe(CompoundTag.CODEC);

    protected WFBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Writes the saved data. Call super first. */
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Reads the saved data. Call super first. */
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Writes the data sent to clients (chunk load and block updates). Call super first. */
    protected void writeSyncData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void readSyncData(CompoundTag tag, HolderLookup.Provider registries) {}

    public void onBlockRemoved() {}

    private HolderLookup.Provider registries() {
        return level != null ? level.registryAccess() : RegistryAccess.EMPTY;
    }

    private static CompoundTag readAll(ValueInput input) {
        return input.read(WHOLE_TAG).orElseGet(CompoundTag::new);
    }

    @Override
    protected final void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        writeData(tag, registries());
        output.store(tag);
    }

    @Override
    protected final void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        readData(readAll(input), input.lookup());
    }

    @Override
    public final void preRemoveSideEffects(BlockPos pos, BlockState state) {
        onBlockRemoved();
        super.preRemoveSideEffects(pos, state);
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
    public final void handleUpdateTag(ValueInput input) {
        readSyncData(readAll(input), input.lookup());
    }

    @Override
    public final void onDataPacket(Connection net, ValueInput input) {
        readSyncData(readAll(input), input.lookup());
    }
}
