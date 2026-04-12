package com.flansmod.warforge.common.network;

import com.flansmod.warforge.server.Faction;

public class ClaimChunkRenderInfo extends SiegeCampAttackInfoRender {
    public final Faction.ClaimType claimType;

    public ClaimChunkRenderInfo(SiegeCampAttackInfo info, Faction.ClaimType claimType) {
        super(info);
        this.claimType = claimType;
    }
}
