package com.flansmod.warforge.client.util;

import com.flansmod.warforge.api.modularui.WFUiElement;
import com.flansmod.warforge.client.render.UiPainters;

import com.flansmod.warforge.client.render.Gui2D;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PlayerFaceElement extends WFUiElement {
    private final UUID playerId;

    public PlayerFaceElement(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    protected void draw(Gui2D graphics) {
        UiPainters.playerFace(graphics, playerId, (int) getPositionX(), (int) getPositionY(),
                (int) Math.min(getSizeWidth(), getSizeHeight()));
    }
}
