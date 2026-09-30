package com.flansmod.warforge.common;

import com.flansmod.warforge.common.util.FactionDisplay;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class WarForgeTabListHandler {

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!WarForgeConfig.FACTION_PREFIX_IN_TABLIST || WarForgeMod.FACTIONS == null) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer self)) {
            return;
        }
        Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(self.getUUID());
        if (faction == null) {
            return;
        }
        Component name = FactionDisplay.tabName(faction, self.getName().getString());
        if (name != null) {
            event.setDisplayName(name);
        }
    }
}
