package com.flansmod.warforge.client.util;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientElements {
    private ClientElements() {
    }

    public static UIElement flag(String flagId) {
        return new FlagElement(flagId);
    }
}
