package com.flansmod.warforge.client.xaero;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.highlight.ChunkHighlighter;

import java.util.List;

/**
 * Colours claimed chunks on Xaero's World Map. Added to its highlighter registry by XaeroWorldMapHighlighterMixin;
 * only loaded when Xaero's World Map is installed.
 */
public class WorldMapClaimHighlighter extends ChunkHighlighter {
    public WorldMapClaimHighlighter() {
        super(true); // also over unexplored map areas, like the JourneyMap overlay
    }

    @Override
    public int calculateRegionHash(ResourceKey<Level> dim, int regionX, int regionZ) {
        return XaeroClaimStyle.regionHash(dim, regionX, regionZ);
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
    public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        return XaeroClaimStyle.tooltip(dim, chunkX, chunkZ);
    }

    @Override
    public Component getChunkHighlightBluntTooltip(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        return null;
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Component> list, ResourceKey<Level> dim, int blockX, int blockZ, int width) {
    }
}
