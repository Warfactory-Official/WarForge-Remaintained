package com.flansmod.warforge.common.blocks;

import java.util.UUID;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.Faction;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface IClaim
{
    public DimBlockPos getClaimPos();
    public BlockEntity getAsBlockEntity();

    public boolean canBeSieged();
    public int getAttackStrength();
    public int getDefenceStrength();
    public int getSupportStrength();

    public void onServerSetFaction(Faction faction);

    // Server side uuid - means nothing to a client
    public UUID getFaction();
    public void updateColour(int colour);

    // Client side data - can't use UUID to identify anything on client
    public int getColour();
    public String getClaimDisplayName();
}
