package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class FactionStatsGuiData {
    public UUID requestedFactionId = FactionIds.NONE;
    public boolean hasFaction;
    public UUID factionId = FactionIds.NONE;
    public String factionName = "";
    public int factionColor = 0xFFFFFF;
    public UUID leaderId = FactionIds.NONE;
    public String leaderName = "";
    public int notoriety;
    public int wealth;
    public int legacy;
    public int total;
    public int notorietyRank;
    public int wealthRank;
    public int legacyRank;
    public int totalRank;
    public int claimCount;
    public int memberCount;
    public int level;
    public int claimLimit;
    public boolean isOwnFaction;
    public boolean canManageMembers;
    public boolean canUpgrade;

    public FactionStatsGuiData(Player player, UUID requestedFactionId) {
        this.requestedFactionId = requestedFactionId;
    }
}
