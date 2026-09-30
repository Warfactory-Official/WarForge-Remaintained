package com.flansmod.warforge.client.util;

import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.UiPainters;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.UUID;

/** Draws a player's face (skin head) in the widget. */
@OnlyIn(Dist.CLIENT)
public class PlayerFaceElement extends Widget {
    private final UUID playerId;

    public PlayerFaceElement(UUID playerId) {
        this(playerId, 18);
    }

    public PlayerFaceElement(UUID playerId, int size) {
        super(0, 0, size, size);
        this.playerId = playerId;
    }

    @Override
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        UiPainters.playerFace(Gui2D.of(graphics), playerId, getPositionX(), getPositionY(),
                Math.min(getSizeWidth(), getSizeHeight()));
    }
}
