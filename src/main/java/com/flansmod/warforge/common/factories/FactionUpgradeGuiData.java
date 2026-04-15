package com.flansmod.warforge.common.factories;

import com.cleanroommc.modularui.factory.GuiData;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.player.EntityPlayer;

import java.util.UUID;

public class FactionUpgradeGuiData extends GuiData {
    public UUID requestedFactionId = Faction.nullUuid;
    public UUID factionId = Faction.nullUuid;
    public String factionName = "";
    public int level;
    public int color = 0xFFFFFF;
    public boolean outrankingOfficer;

    public FactionUpgradeGuiData(EntityPlayer player, UUID requestedFactionId) {
        super(player);
        this.requestedFactionId = requestedFactionId;
    }
}
