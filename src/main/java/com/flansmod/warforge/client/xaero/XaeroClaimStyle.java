package com.flansmod.warforge.client.xaero;

import com.flansmod.warforge.client.ClaimMapStyle;
import com.flansmod.warforge.client.ClientBorderCache;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Map threads: reads only ClientBorderCache snapshots. */
public final class XaeroClaimStyle {
    private static final int NO_CLAIM = ClientBorderCache.NO_CLAIM;

    private XaeroClaimStyle() {
    }

    /** null => unclaimed. [0] fill, [1] N(-Z), [2] E(+X), [3] S(+Z), [4] W(-X) border; each 0xBBGGRRAA. */
    static int[] colors(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        int colour = ClientBorderCache.claimColour(dim, chunkX, chunkZ);
        if (colour == NO_CLAIM) {
            return null;
        }
        int fill = xaeroColour(colour, ClaimMapStyle.fillAlpha(colour));
        int border = xaeroColour(colour, ClaimMapStyle.borderAlpha(colour));
        return new int[]{
                fill,
                sameClaim(colour, dim, chunkX, chunkZ - 1) ? 0 : border,
                sameClaim(colour, dim, chunkX + 1, chunkZ) ? 0 : border,
                sameClaim(colour, dim, chunkX, chunkZ + 1) ? 0 : border,
                sameClaim(colour, dim, chunkX - 1, chunkZ) ? 0 : border,
        };
    }

    static boolean isClaimed(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        return ClientBorderCache.claimColour(dim, chunkX, chunkZ) != NO_CLAIM;
    }

    static boolean regionHasClaims(ResourceKey<Level> dim, int regionX, int regionZ) {
        return ClientBorderCache.regionHasClaims(dim, regionX, regionZ);
    }

    static int regionHash(ResourceKey<Level> dim, int regionX, int regionZ) {
        return ClientBorderCache.regionRevision(dim, regionX, regionZ);
    }

    static Component tooltip(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        int colour = ClientBorderCache.claimColour(dim, chunkX, chunkZ);
        if (colour == NO_CLAIM) {
            return null;
        }
        return ClaimMapStyle.label(chunkX, chunkZ).copy().withStyle(style -> style.withColor(colour & 0xFFFFFF));
    }

    private static boolean sameClaim(int colour, ResourceKey<Level> dim, int chunkX, int chunkZ) {
        int other = ClientBorderCache.claimColour(dim, chunkX, chunkZ);
        return other != NO_CLAIM && ClaimMapStyle.sharesBorder(colour, other);
    }

    static int xaeroColour(int argb, int alpha) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return b << 24 | g << 16 | r << 8 | alpha;
    }
}
