package com.flansmod.warforge.client.util;

import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.UiPainters;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/** Draws a faction flag scaled into the widget. */
@OnlyIn(Dist.CLIENT)
public class FlagElement extends Widget {
    private String flagId;

    public FlagElement(String flagId) {
        this(flagId, 42, 24);
    }

    public FlagElement(String flagId, int width, int height) {
        super(0, 0, width, height);
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
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        UiPainters.flag(Gui2D.of(graphics), flagId, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
    }
}
