package com.flansmod.warforge.api.modularui;

import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.MapCellPainter;
import com.flansmod.warforge.client.ui.model.SiegeCampAttackInfoRender;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class MapCellElement extends Widget {
    private final String mapData;
    private final SiegeCampAttackInfoRender chunkState;
    private final boolean[] adjacency;
    private final boolean campChunk;
    private final float[] rgb;

    public Consumer<Integer> onClick = null;

    private @Nullable Supplier<List<Component>> lazyTooltip;

    public MapCellElement tooltipOnHover(Supplier<List<Component>> supplier) {
        this.lazyTooltip = supplier;
        return this;
    }

    @Override
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (lazyTooltip != null && isMouseOverElement(mouseX, mouseY)) {
            setHoverTooltips(lazyTooltip.get());
            lazyTooltip = null;
        }
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
    }

    public MapCellElement(String mapData, SiegeCampAttackInfoRender chunkState, boolean[] adjacency, int size) {
        super(0, 0, size, size);
        this.mapData = mapData;
        this.chunkState = chunkState;
        this.adjacency = adjacency;
        this.campChunk = MapCellPainter.isCampChunk(chunkState);
        this.rgb = MapCellPainter.factionRgb(chunkState);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOverElement(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (campChunk && !chunkState.canAttack) {
            return true;
        }
        if (onClick != null) {
            onClick.accept(button);
        }
        return true;
    }

    @Override
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        MapCellPainter.paint(Gui2D.of(graphics), mapData, chunkState, adjacency, campChunk, rgb,
                getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), isMouseOverElement(mouseX, mouseY));
    }
}
