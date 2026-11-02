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

public abstract class WFBlockEntity extends BlockEntity {
    protected WFBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void writeSyncData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void readSyncData(CompoundTag tag, HolderLookup.Provider registries) {}

    public void onBlockRemoved() {}

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

    @Override
    public AABB getRenderBoundingBox() {
        AABB box = RenderBounds.of(this);
        return box != null ? box : super.getRenderBoundingBox();
    }
}
