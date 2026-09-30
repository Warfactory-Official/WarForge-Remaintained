package com.flansmod.warforge.client.util;

import com.flansmod.warforge.api.modularui.WFUiElement;
import com.flansmod.warforge.client.render.UiPainters;

import com.flansmod.warforge.client.render.Gui2D;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlagElement extends WFUiElement {
    private String flagId;

    public FlagElement(String flagId) {
        this.flagId = flagId;
    }

    public FlagElement setFlagId(String flagId) {
        this.flagId = flagId;
        return this;
    }

    public String getFlagId() {
        return flagId;
    }

    @Override
    protected void draw(Gui2D graphics) {
        UiPainters.flag(graphics, flagId, (int) getPositionX(), (int) getPositionY(),
                (int) getSizeWidth(), (int) getSizeHeight());
    }
}
