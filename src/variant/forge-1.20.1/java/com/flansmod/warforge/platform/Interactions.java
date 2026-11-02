package com.flansmod.warforge.platform;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

public final class Interactions {
    private Interactions() {}

    public static boolean isUseItemDenied(PlayerInteractEvent.RightClickBlock event) {
        return event.getUseItem() == Event.Result.DENY;
    }

    public static void denyUseItem(PlayerInteractEvent.RightClickBlock event) {
        event.setUseItem(Event.Result.DENY);
    }
}
