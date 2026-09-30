package com.flansmod.warforge.common.ui;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The LDLib1 UI holder of a WarForge block UI: the block at a position, plus the display state the server captured
 * when opening (sent to the client in the open packet so both sides build the same widget tree).
 */
public final class BlockUiHolder implements IUIHolder {
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    public final Level level;
    public final BlockPos pos;
    public final Block block;
    public final CompoundTag state;
    private Player player;

    public BlockUiHolder(Level level, BlockPos pos, Block block, CompoundTag state) {
        this.level = level;
        this.pos = pos;
        this.block = block;
        this.state = state;
    }

    public BlockEntity blockEntity() {
        return level.getBlockEntity(pos);
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        this.player = entityPlayer;
        return BlockUis.create(this, entityPlayer);
    }

    @Override
    public boolean isInvalid() {
        if (level.getBlockState(pos).getBlock() != block) {
            return true;
        }
        return player != null && player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR;
    }

    @Override
    public boolean isRemote() {
        return level.isClientSide;
    }

    @Override
    public void markAsDirty() {
        BlockEntity be = blockEntity();
        if (be != null) {
            be.setChanged();
        }
    }
}
