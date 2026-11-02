package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import com.flansmod.warforge.platform.WFBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.flansmod.warforge.compat.Nbt;

public class DummyBlockEntity extends WFBlockEntity implements IBlockDummy {
    protected String playerFlag;
    private BlockPos masterPos;
    private boolean canRenderLaser = false;
    private float[] laserRGB = new float[]{0F, 0F, 0F};

    public DummyBlockEntity(BlockPos pos, BlockState state) {
        super(Content.TE_DUMMY.get(), pos, state);
    }

    public void setMaster(BlockPos pos) {
        this.masterPos = pos;
        setChanged();
    }

    public boolean getLaserRender() {
        return canRenderLaser;
    }

    public void setLaserRender(boolean bool) {
        canRenderLaser = bool;
        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, 3);
        level.scheduleTick(worldPosition, getBlockState().getBlock(), 0);
        setChanged();
    }

    public float[] getLaserRGB() {
        return laserRGB;
    }

    public void setLaserRGB(int colour) {
        float r = ((colour >> 16) & 0xFF) / 255.0f;
        float g = ((colour >> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;
        this.laserRGB = new float[]{r, g, b};
        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, 3);
        level.scheduleTick(worldPosition, getBlockState().getBlock(), 0);
        setChanged();
    }

    public void setLaserRGB(float[] laserRGB) {
        this.laserRGB = laserRGB;
    }

    public BlockPos getMasterTile() {
        if (level == null || masterPos == null) return null;
        return masterPos;
    }

    @Override
    protected void writeData(CompoundTag compound, HolderLookup.Provider registries) {
        super.writeData(compound, registries);
        compound.putBoolean("laser", canRenderLaser);
        compound.putFloat("r", laserRGB[0]);
        compound.putFloat("g", laserRGB[1]);
        compound.putFloat("b", laserRGB[2]);
        if (masterPos != null) {
            compound.putLong("master", masterPos.asLong());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide()) return;
        if (masterPos == null) {
            WarForgeMod.LOGGER.warn("Dummy block at {} has no master; searching below", worldPosition);
            for (int i = 1; i <= 3; i++) {
                Block block = level.getBlockState(worldPosition.below(i)).getBlock();
                if (block instanceof MultiBlockColumn) {
                    setMaster(worldPosition.below(i));
                    WarForgeMod.LOGGER.info("Dummy block at {} found master at {}", worldPosition, worldPosition.below(i));
                    setChanged();
                    return;
                }
                WarForgeMod.LOGGER.error("Dummy block at {} cannot find its master", worldPosition);
            }
        }
    }

    @Override
    protected void readData(CompoundTag compound, HolderLookup.Provider registries) {
        super.readData(compound, registries);
        canRenderLaser = Nbt.getBoolean(compound, "laser");
        laserRGB[0] = Nbt.getFloat(compound, "r");
        laserRGB[1] = Nbt.getFloat(compound, "g");
        laserRGB[2] = Nbt.getFloat(compound, "b");

        if (compound.contains("master")) {
            masterPos = BlockPos.of(Nbt.getLong(compound, "master"));
        }
    }

    @Override
    protected void writeSyncData(CompoundTag tags, HolderLookup.Provider registries) {
        super.writeSyncData(tags, registries);
        tags.putBoolean("laser", canRenderLaser);
        tags.putFloat("r", laserRGB[0]);
        tags.putFloat("g", laserRGB[1]);
        tags.putFloat("b", laserRGB[2]);
        if (masterPos != null)
            tags.putLong("master", masterPos.asLong());
    }

    @Override
    protected void readSyncData(CompoundTag tags, HolderLookup.Provider registries) {
        canRenderLaser = Nbt.getBoolean(tags, "laser");
        laserRGB[0] = Nbt.getFloat(tags, "r");
        laserRGB[1] = Nbt.getFloat(tags, "g");
        laserRGB[2] = Nbt.getFloat(tags, "b");
        masterPos = BlockPos.of(Nbt.getLong(tags, "master"));
    }

}
