package com.flansmod.warforge.common.factories;

import brachy.modularui.factory.GuiData;
import com.flansmod.warforge.server.Faction;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class FactionUpgradeGuiData extends GuiData {
    public UUID requestedFactionId = Faction.nullUuid;
    public UUID factionId = Faction.nullUuid;
    public String factionName = "";
    public int level;
    public int color = 0xFFFFFF;
    public boolean outrankingOfficer;

    public FactionUpgradeGuiData(Player player, UUID requestedFactionId) {
        super(player);
        this.requestedFactionId = requestedFactionId;
    }
}
