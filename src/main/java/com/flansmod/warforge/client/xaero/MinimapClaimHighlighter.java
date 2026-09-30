package com.flansmod.warforge.client.xaero;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.highlight.ChunkHighlighter;
import xaero.hud.minimap.info.render.compile.InfoDisplayCompiler;

/**
 * Colours claimed chunks on Xaero's Minimap. Added to its highlighter registry by XaeroMinimapHighlighterMixin; only
 * loaded when Xaero's Minimap is installed.
 */
public class MinimapClaimHighlighter extends ChunkHighlighter {
    public MinimapClaimHighlighter() {
        super(true);
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dim, int regionX, int regionZ) {
        return XaeroClaimStyle.regionHasClaims(dim, regionX, regionZ);
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        return XaeroClaimStyle.isClaimed(dim, chunkX, chunkZ);
    }

    @Override
    protected int[] getColors(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        return XaeroClaimStyle.colors(dim, chunkX, chunkZ);
    }

    @Override
    public void addChunkHighlightTooltips(InfoDisplayCompiler compiler, ResourceKey<Level> dim, int chunkX, int chunkZ, int width) {
        Component tooltip = XaeroClaimStyle.tooltip(dim, chunkX, chunkZ);
        if (tooltip != null) {
            compiler.addLine(tooltip);
        }
    }
}
