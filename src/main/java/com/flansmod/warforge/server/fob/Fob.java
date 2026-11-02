package com.flansmod.warforge.server.fob;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Nbt;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class Fob {
    public DimBlockPos pos = DimBlockPos.ZERO;
    public UUID ownerFaction = FactionIds.NONE;
    public String name = "";
    public int tickets = 0;
    public int maxTickets = 0;
    public int enemyHoldTicks = 0;

    public Fob() {
    }

    public Fob(DimBlockPos pos, UUID ownerFaction, String name) {
        this.pos = pos;
        this.ownerFaction = ownerFaction;
        this.name = name == null ? "" : name;
    }

    public DimChunkPos toChunkPos() {
        return pos.toChunkPos();
    }

    public void writeToNBT(CompoundTag tags) {
        pos.writeToNBT(tags, "pos");
        tags.putString("name", name);
        tags.putInt("tickets", tickets);
        tags.putInt("maxTickets", maxTickets);
        tags.putInt("enemyHoldTicks", enemyHoldTicks);
    }

    public void readFromNBT(CompoundTag tags) {
        pos = DimBlockPos.readFromNBT(tags, "pos");
        name = Nbt.getString(tags, "name");
        tickets = Nbt.getInt(tags, "tickets");
        maxTickets = Nbt.getInt(tags, "maxTickets");
        enemyHoldTicks = Nbt.getInt(tags, "enemyHoldTicks");
    }
}
