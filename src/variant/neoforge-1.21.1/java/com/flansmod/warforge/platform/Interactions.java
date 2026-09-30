package com.flansmod.warforge.platform;

import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Right-click event results. The result type differs between versions. */
public final class Interactions {
    private Interactions() {}

    /** True if something already stopped the held item from being used. */
    public static boolean isUseItemDenied(PlayerInteractEvent.RightClickBlock event) {
        return event.getUseItem() == TriState.FALSE;
    }

    /** Stops the held item from being used (the block can still be used). */
    public static void denyUseItem(PlayerInteractEvent.RightClickBlock event) {
        event.setUseItem(TriState.FALSE);
    }
}
