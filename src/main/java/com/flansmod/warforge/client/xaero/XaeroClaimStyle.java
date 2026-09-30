package com.flansmod.warforge.client.xaero;

import com.flansmod.warforge.client.MinimapClaimCache;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Claim colours and texts for the Xaero's World Map and Minimap highlighters (both use the same colour layout).
 * Only reads {@link MinimapClaimCache}, so it is safe on the map threads.
 */
public final class XaeroClaimStyle {
    /** Chunks per Xaero map region side (a region is 32 x 32 chunks). */
    static final int REGION_CHUNKS = 32;

    private XaeroClaimStyle() {
    }

    /**
     * The highlight of a chunk in Xaero's layout, or null if it is not claimed: [0] fill, [1] north (-Z) border,
     * [2] east (+X), [3] south (+Z), [4] west (-X). Each value is 0xBBGGRRAA; a border is drawn only on a side that
     * faces a different claim or unclaimed land. The opacities are the ones of the JourneyMap overlay (fill at most
     * 40%, border at least 70% of the claim colour's alpha).
     */
    static int[] colors(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        Integer colour = MinimapClaimCache.colourAt(dim, chunkX, chunkZ);
        if (colour == null) {
            return null;
        }
        int alpha = colour >>> 24;
        int fill = xaeroColour(colour, Math.min(alpha, 102));
        int border = xaeroColour(colour, Math.max(alpha, 179));
        return new int[]{
                fill,
                sameClaim(colour, dim, chunkX, chunkZ - 1) ? 0 : border,
                sameClaim(colour, dim, chunkX + 1, chunkZ) ? 0 : border,
                sameClaim(colour, dim, chunkX, chunkZ + 1) ? 0 : border,
                sameClaim(colour, dim, chunkX - 1, chunkZ) ? 0 : border,
        };
    }

    static boolean isClaimed(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        return MinimapClaimCache.colourAt(dim, chunkX, chunkZ) != null;
    }

    static boolean regionHasClaims(ResourceKey<Level> dim, int regionX, int regionZ) {
        int x0 = regionX * REGION_CHUNKS;
        int z0 = regionZ * REGION_CHUNKS;
        for (int x = x0; x < x0 + REGION_CHUNKS; x++) {
            for (int z = z0; z < z0 + REGION_CHUNKS; z++) {
                if (isClaimed(dim, x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A hash of everything that changes how a region looks: the claims in it and in the chunk ring around it (the
     * neighbours decide where its borders are).
     */
    static int regionHash(ResourceKey<Level> dim, int regionX, int regionZ) {
        int x0 = regionX * REGION_CHUNKS;
        int z0 = regionZ * REGION_CHUNKS;
        int hash = 1;
        for (int x = x0 - 1; x <= x0 + REGION_CHUNKS; x++) {
            for (int z = z0 - 1; z <= z0 + REGION_CHUNKS; z++) {
                Integer colour = MinimapClaimCache.colourAt(dim, x, z);
                hash = 31 * hash + (colour == null ? 0 : colour | 1);
            }
        }
        return hash;
    }

    /** The hover text of a claimed chunk (only the position: the payload carries no faction names). */
    static Component tooltip(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        Integer colour = MinimapClaimCache.colourAt(dim, chunkX, chunkZ);
        if (colour == null) {
            return null;
        }
        return Component.literal(String.format("Claim [%s, %s]", chunkX, chunkZ)).withStyle(style -> style.withColor(colour & 0xFFFFFF));
    }

    private static boolean sameClaim(int colour, ResourceKey<Level> dim, int chunkX, int chunkZ) {
        Integer other = MinimapClaimCache.colourAt(dim, chunkX, chunkZ);
        return other != null && other == colour;
    }

    /** ARGB colour with the given alpha, as Xaero's 0xBBGGRRAA. */
    private static int xaeroColour(int argb, int alpha) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return b << 24 | g << 16 | r << 8 | alpha;
    }
}
