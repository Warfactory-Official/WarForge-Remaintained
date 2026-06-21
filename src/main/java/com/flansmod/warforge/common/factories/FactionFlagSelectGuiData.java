package com.flansmod.warforge.common.factories;

import brachy.modularui.factory.GuiData;
import com.flansmod.warforge.server.Faction;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FactionFlagSelectGuiData extends GuiData {
    public UUID factionId = Faction.nullUuid;
    public String factionName = "";
    public int factionColor = 0xFFFFFF;
    public String currentFlagId = "";
    public boolean canChoose;
    public final List<String> availableFlags = new ArrayList<>();

    public FactionFlagSelectGuiData(Player player, UUID factionId) {
        super(player);
        this.factionId = factionId;
    }
}
