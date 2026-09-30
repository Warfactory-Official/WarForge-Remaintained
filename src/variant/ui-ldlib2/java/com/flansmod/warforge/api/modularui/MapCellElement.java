package com.flansmod.warforge.api.modularui;

import com.flansmod.warforge.client.render.MapCellPainter;

import com.flansmod.warforge.client.render.Gui2D;

import com.flansmod.warforge.common.network.SiegeCampAttackInfoRender;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class MapCellElement extends WFUiElement {

    private final String mapData;
    private final SiegeCampAttackInfoRender chunkState;
    private final boolean[] adjacency;
    private final boolean campChunk;
    private final float[] rgb;

    public Consumer<Integer> onClick = null;

    public MapCellElement(String mapData, SiegeCampAttackInfoRender chunkState, boolean[] adjacency) {
        this.mapData = mapData;
        this.chunkState = chunkState;
        this.adjacency = adjacency;
        this.campChunk = MapCellPainter.isCampChunk(chunkState);
        this.rgb = MapCellPainter.factionRgb(chunkState);

        addEventListener(UIEvents.CLICK, (UIEventListener) event -> {
            if (campChunk && !chunkState.canAttack) {
                return;
            }
            if (onClick != null) {
                onClick.accept(event.button);
            }
        });
    }

    @Override
    protected void draw(Gui2D graphics) {
        MapCellPainter.paint(graphics, mapData, chunkState, adjacency, campChunk, rgb,
                (int) getPositionX(), (int) getPositionY(), (int) getSizeWidth(), (int) getSizeHeight(), isHover());
    }
}
