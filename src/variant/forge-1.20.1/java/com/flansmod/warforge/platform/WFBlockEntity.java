package com.flansmod.warforge.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.flansmod.warforge.client.render.RenderBounds;

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

    /** 1.20.1 saves without registries; item stacks do not need them here. */
    private HolderLookup.Provider registries() {
        return level != null ? level.registryAccess() : RegistryAccess.EMPTY;
    }

    @Override
    protected final void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeData(tag, registries());
    }

    @Override
    public final void load(CompoundTag tag) {
        super.load(tag);
        readData(tag, registries());
    }

    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public final CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeSyncData(tag, registries());
        return tag;
    }

    @Override
    public final void handleUpdateTag(CompoundTag tag) {
        readSyncData(tag, registries());
    }

    @Override
    public final void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        readSyncData(tag == null ? new CompoundTag() : tag, registries());
    }

    /** Forge 1.20.1 asks the block entity for its render box (NeoForge asks the renderer). */
    @Override
    public AABB getRenderBoundingBox() {
        AABB box = RenderBounds.of(this);
        return box != null ? box : super.getRenderBoundingBox();
    }
}
