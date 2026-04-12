package com.flansmod.warforge.common.network;

import net.minecraft.util.ResourceLocation;
import com.flansmod.warforge.server.Faction;

public class ClaimChunkRenderInfo extends SiegeCampAttackInfoRender {
    public final Faction.ClaimType claimType;
    public final boolean forceLoaded;

    public ClaimChunkRenderInfo(SiegeCampAttackInfo info, Faction.ClaimType claimType, boolean forceLoaded, ResourceLocation centerIcon) {
        super(info);
        this.claimType = claimType;
        this.forceLoaded = forceLoaded;
        if (centerIcon != null) {
            setCenterMarkType(CenterMarkType.PLAYER_FACE);
            setCenterIcon(centerIcon);
        }
    }
}
