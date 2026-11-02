package com.flansmod.warforge.client;

import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** The protection config the server resolved for this player in a chunk; the server stays authoritative. */
public final class ClientProtections {
    private ClientProtections() {
    }

    /** Null while the chunk is outside the synced window or nothing applies to this player: defer to the server. */
    public static ProtectionConfig configFor(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        ClaimChunkInfo info = ClientBorderCache.get(dim, chunkX, chunkZ);
        return info == null ? null : info.protection.config();
    }
}
