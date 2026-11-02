package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.flansmod.warforge.common.util.DimBlockPos;

public class FactionDisplayInfo
{
    public UUID factionId = FactionIds.NONE;
    public String factionName = "";
    public int colour = 0xFFFFFF;
    public String flagId = "";
    public UUID leaderId = FactionIds.NONE;
    public List<PlayerDisplayInfo> members = new ArrayList<>();

    public int notoriety = 0;
    public int wealth = 0;
    public int legacy = 0;
    public int lvl = 0;

    public int notorietyRank = 0;
    public int wealthRank = 0;
    public int legacyRank = 0;
    public int totalRank = 0;

    public int numClaims = 0;
    public int numActiveSiegeCamps = 0;
    public int numActiveLeeches = 0;
    public DimBlockPos citadelPos = DimBlockPos.ZERO;

    public PlayerDisplayInfo getPlayerInfo(UUID playerID)
    {
        for(PlayerDisplayInfo info : members)
        {
            if(info.playerUuid.equals(playerID))
                return info;
        }
        return null;
    }
}
