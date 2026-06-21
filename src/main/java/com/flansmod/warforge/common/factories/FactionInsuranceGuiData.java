package com.flansmod.warforge.common.factories;

import brachy.modularui.factory.GuiData;
import com.flansmod.warforge.server.Faction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.UUID;

public class FactionInsuranceGuiData extends GuiData {
    public UUID requestedFactionId = Faction.nullUuid;
    public boolean hasFaction;
    public UUID factionId = Faction.nullUuid;
    public String factionName = "";
    public int factionColor = 0xFFFFFF;
    public boolean canDeposit;
    public boolean canVoid;
    public int slotCount;
    public IItemHandlerModifiable insuranceHandler;

    public FactionInsuranceGuiData(Player player, UUID requestedFactionId) {
        super(player);
        this.requestedFactionId = requestedFactionId;
    }
}
