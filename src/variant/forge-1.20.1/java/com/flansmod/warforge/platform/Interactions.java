package com.flansmod.warforge.platform;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

/** Right-click event results. The result type differs between versions. */
public final class Interactions {
    private Interactions() {}

    /** True if something already stopped the held item from being used. */
    public static boolean isUseItemDenied(PlayerInteractEvent.RightClickBlock event) {
        return event.getUseItem() == Event.Result.DENY;
    }

    /** Stops the held item from being used (the block can still be used). */
    public static void denyUseItem(PlayerInteractEvent.RightClickBlock event) {
        event.setUseItem(Event.Result.DENY);
    }
}
