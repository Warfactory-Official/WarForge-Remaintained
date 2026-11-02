package com.flansmod.warforge.common.blocks;


import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import com.flansmod.warforge.platform.WFBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.flansmod.warforge.compat.Nbt;

public class LeaderboardBlockEntity extends WFBlockEntity
{
    public static final int NUM_ENTRIES = 6;
    public String[] topNames = new String[NUM_ENTRIES];

    public LeaderboardBlockEntity(BlockPos pos, BlockState state)
    {
        super(Content.TE_LEADERBOARD.get(), pos, state);
    }

    @Override
    protected void writeSyncData(CompoundTag tags, HolderLookup.Provider registries)
    {
        super.writeSyncData(tags, registries);

        java.util.List<Faction> tempList = WarForgeMod.LEADERBOARD.getSortedList(((BlockLeaderboard)getBlockState().getBlock()).stat);

        for(int i = 0; i < NUM_ENTRIES; i++)
        {
            if(tempList.size() > i)
            {
                tags.putString("#" + i, tempList.get(i).getName());
            }
            else
            {
                tags.putString("#" + i, "");
            }
        }
    }

    @Override
    protected void readSyncData(CompoundTag tags, HolderLookup.Provider registries)
    {
        for(int i = 0; i < NUM_ENTRIES; i++)
        {
            topNames[i] = Nbt.getString(tags, "#" + i);
        }
    }
}
