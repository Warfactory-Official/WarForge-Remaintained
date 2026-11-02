package com.flansmod.warforge.client.util;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientElements {
    private ClientElements() {
    }

    public static Widget flag(String flagId, int width, int height) {
        return new FlagElement(flagId, width, height);
    }
}
