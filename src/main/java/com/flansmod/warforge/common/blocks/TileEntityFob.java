package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.fob.Fob;
import com.flansmod.warforge.server.fob.FobPresence;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.flansmod.warforge.platform.WFBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;
import com.flansmod.warforge.compat.Nbt;

public class TileEntityFob extends WFBlockEntity {
    public UUID ownerFaction = new UUID(0, 0);
    public UUID placer = new UUID(0, 0);
    public String name = "";
    public int tickets = 0;
    public int maxTickets = 0;
    public String factionFlagId = "";
    public float poleLength = TileEntityClaim.DEFAULT_POLE_LENGTH;
    private DimChunkPos cachedChunkPos;

    public TileEntityFob(BlockPos pos, BlockState state) {
        super(Content.TE_FOB.get(), pos, state);
    }

    public float getPoleLength() {
        return poleLength;
    }

    public void setPoleLength(float length) {
        poleLength = length;
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            setChanged();
        }
    }

    public void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (ownerFaction.equals(Faction.nullUuid)) {
            return;
        }

        if (cachedChunkPos == null) cachedChunkPos = new DimChunkPos(level.dimension(), getBlockPos());
        Fob fob = WarForgeMod.FOBS.getFobAt(cachedChunkPos);
        if (fob == null) {
            return;
        }

        boolean changed = false;
        if (tickets != fob.tickets || maxTickets != fob.maxTickets) {
            tickets = fob.tickets;
            maxTickets = fob.maxTickets;
            changed = true;
        }

        Faction owner = WarForgeMod.FACTIONS.getFaction(ownerFaction);
        String ownerFlag = owner == null ? "" : owner.flagId;
        if (!factionFlagId.equals(ownerFlag)) {
            factionFlagId = ownerFlag;
            changed = true;
        }

        if (changed) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }

        int threshold = WarForgeMod.FOBS.getSiegeHoldThresholdTicks(fob);
        if (threshold <= 0) {
            if (fob.enemyHoldTicks > 0) {
                fob.enemyHoldTicks--;
            }
            return;
        }

        boolean enemyHeld = FobPresence.enemyHeld(fob);

        if (enemyHeld && !FobPresence.ownerOrAllyAbove(fob)) {
            fob.enemyHoldTicks++;
            if (fob.enemyHoldTicks >= threshold) {
                WarForgeMod.FOBS.destroyFob(fob);
            }
        } else if (fob.enemyHoldTicks > 0) {
            fob.enemyHoldTicks--;
        }
    }

    @Override
    protected void writeData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeData(nbt, registries);
        Nbt.putUUID(nbt, "ownerFaction", ownerFaction);
        Nbt.putUUID(nbt, "placer", placer);
        nbt.putString("name", name);
        nbt.putInt("tickets", tickets);
        nbt.putInt("maxTickets", maxTickets);
        nbt.putString("flagId", factionFlagId);
        nbt.putFloat("poleLength", poleLength);
    }

    @Override
    protected void readData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.readData(nbt, registries);
        ownerFaction = Nbt.hasUUID(nbt, "ownerFaction") ? Nbt.getUUID(nbt, "ownerFaction") : Faction.nullUuid;
        placer = Nbt.hasUUID(nbt, "placer") ? Nbt.getUUID(nbt, "placer") : Faction.nullUuid;
        name = Nbt.getString(nbt, "name");
        tickets = Nbt.getInt(nbt, "tickets");
        maxTickets = Nbt.getInt(nbt, "maxTickets");
        factionFlagId = Nbt.getString(nbt, "flagId");
        poleLength = nbt.contains("poleLength") ? Nbt.getFloat(nbt, "poleLength") : TileEntityClaim.DEFAULT_POLE_LENGTH;
    }

    @Override
    protected void writeSyncData(CompoundTag tags, HolderLookup.Provider registries) {
        super.writeSyncData(tags, registries);
        Nbt.putUUID(tags, "ownerFaction", ownerFaction);
        tags.putString("name", name);
        tags.putInt("tickets", tickets);
        tags.putInt("maxTickets", maxTickets);
        tags.putString("flagId", factionFlagId);
        tags.putFloat("poleLength", poleLength);
    }

    @Override
    protected void readSyncData(CompoundTag tags, HolderLookup.Provider registries) {
        ownerFaction = Nbt.hasUUID(tags, "ownerFaction") ? Nbt.getUUID(tags, "ownerFaction") : Faction.nullUuid;
        name = Nbt.getString(tags, "name");
        tickets = Nbt.getInt(tags, "tickets");
        maxTickets = Nbt.getInt(tags, "maxTickets");
        factionFlagId = Nbt.getString(tags, "flagId");
        poleLength = tags.contains("poleLength") ? Nbt.getFloat(tags, "poleLength") : TileEntityClaim.DEFAULT_POLE_LENGTH;
    }
}
