package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.flansmod.warforge.platform.WFBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

import static com.flansmod.warforge.common.blocks.BlockCitadel.FACING;
import com.flansmod.warforge.compat.Nbt;

public abstract class ClaimBlockEntity extends WFBlockEntity implements IClaim {
    /** Display copies of the owning faction: resolved from FACTIONS on server load, synced to clients. */
    public int colour = 0xFF_FF_FF;
    public String factionName = "";
    public String factionFlagId = "";
    protected UUID factionUUID = FactionIds.NONE;
    public byte rotation;

    public static final float DEFAULT_POLE_LENGTH = 8.75F;
    public float poleLength = DEFAULT_POLE_LENGTH;

    public ClaimBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public UUID getFaction() {
        return factionUUID;
    }

    public void increaseRotation() {
        rotation += 1;
        rotation  = (byte) (rotation % 8);
        updateTileEntity();
    }

    public float getPoleLength() {
        return poleLength;
    }

    public void setPoleLength(float length) {
        poleLength = length;
        updateTileEntity();
    }

    public void onPlacedBy(LivingEntity placer) {
        Direction blockRotation = level.getBlockState(worldPosition).getValue(FACING);
        rotation = switch (blockRotation) {
            case EAST -> 6;
            case SOUTH -> 4;
            case WEST -> 2;
            default -> 0;
        };
    }

    @Override
    public void updateColour(int colour) {
        this.colour = colour;
        BlockEntity te = level.getBlockEntity(worldPosition.above(2));
        if (te instanceof DummyBlockEntity) {
            ((DummyBlockEntity) te).setLaserRGB(colour);
        }
        updateTileEntity();
    }

    @Override
    public int getColour() {
        return colour;
    }

    @Override
    public BlockEntity getAsBlockEntity() {
        return this;
    }

    @Override
    public DimBlockPos getClaimPos() {
        return new DimBlockPos(level.dimension(), getBlockPos());
    }

    @Override
    public boolean canBeSieged() {
        return true;
    }

    @Override
    public String getClaimDisplayName() {
        return factionName;
    }

    @Override
    public void onServerSetFaction(Faction faction) {
        if (faction == null) {
            factionUUID = FactionIds.NONE;
            factionFlagId = "";
        } else {
            factionUUID = faction.getUuid();
            updateColour(faction.getColour());
            factionName = faction.getName();
            factionFlagId = faction.getFlagId();
        }

        if (level.getBlockState(worldPosition).getBlock() instanceof MultiBlockColumn b) {
            b.setUpMultiblock(level, worldPosition, b.defaultBlockState());
        }

        updateTileEntity();
    }

    public void updateFactionName(String newName) {
        factionName = newName == null ? "" : newName;
        updateTileEntity();
    }

    public void updateFactionFlag(String newFlagId) {
        factionFlagId = newFlagId == null ? "" : newFlagId;
        updateTileEntity();
    }

    private void updateTileEntity() {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, 3);
        setChanged();
    }

    @Override
    protected void writeData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeData(nbt, registries);

        Nbt.putUUID(nbt, "faction", factionUUID);
        nbt.putByte("rotation", rotation);
        nbt.putFloat("poleLength", poleLength);
    }

    @Override
    protected void readData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.readData(nbt, registries);

        factionUUID = Nbt.getUUID(nbt, "faction");
        rotation = Nbt.getByte(nbt, "rotation");
        poleLength = nbt.contains("poleLength") ? Nbt.getFloat(nbt, "poleLength") : DEFAULT_POLE_LENGTH;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide()) {
            return;
        }
        Faction faction = WarForgeMod.FACTIONS.registry.getFaction(factionUUID);
        if (!factionUUID.equals(FactionIds.NONE) && faction == null) {
            WarForgeMod.LOGGER.warn("Faction {} not found for claim block at {}", factionUUID, worldPosition);
        }
        if (faction != null) {
            colour = faction.getColour();
            factionName = faction.getName();
            factionFlagId = faction.getFlagId();
        }
    }

    @Override
    protected void writeSyncData(CompoundTag tags, HolderLookup.Provider registries) {
        super.writeSyncData(tags, registries);

        Nbt.putUUID(tags, "faction", factionUUID);
        tags.putInt("colour", colour);
        tags.putString("name", factionName);
        tags.putString("flagId", factionFlagId);
        tags.putByte("rotation", rotation);
        tags.putFloat("poleLength", poleLength);
    }

    @Override
    protected void readSyncData(CompoundTag tags, HolderLookup.Provider registries) {
        factionUUID = Nbt.getUUID(tags, "faction");
        colour = Nbt.getInt(tags, "colour");
        factionName = Nbt.getString(tags, "name");
        factionFlagId = Nbt.getString(tags, "flagId");
        rotation = Nbt.getByte(tags, "rotation");
        poleLength = tags.contains("poleLength") ? Nbt.getFloat(tags, "poleLength") : DEFAULT_POLE_LENGTH;
    }
}
