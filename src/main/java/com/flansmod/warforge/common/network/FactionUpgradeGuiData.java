package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class FactionUpgradeGuiData {
    public UUID requestedFactionId = FactionIds.NONE;
    public UUID factionId = FactionIds.NONE;
    public String factionName = "";
    public int level;
    public int color = 0xFFFFFF;
    public boolean outrankingOfficer;

    public FactionUpgradeGuiData(Player player, UUID requestedFactionId) {
        this.requestedFactionId = requestedFactionId;
    }
}
