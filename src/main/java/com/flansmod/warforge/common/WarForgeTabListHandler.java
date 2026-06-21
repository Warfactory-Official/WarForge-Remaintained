package com.flansmod.warforge.common;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.util.FactionDisplay;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Replaces a player's tab-list display name with their faction-prefixed name when the
 * {@code Faction Prefix In Tab List} setting is enabled. Leaving the display name untouched
 * keeps the vanilla plain name.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
