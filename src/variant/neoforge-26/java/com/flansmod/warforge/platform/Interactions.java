package com.flansmod.warforge.platform;

import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class Interactions {
    private Interactions() {}

    public static boolean isUseItemDenied(PlayerInteractEvent.RightClickBlock event) {
        return event.getUseItem() == TriState.FALSE;
    }

    public static void denyUseItem(PlayerInteractEvent.RightClickBlock event) {
        event.setUseItem(TriState.FALSE);
    }
}
