package com.flansmod.warforge.client.ui.model;

import com.flansmod.warforge.common.network.SiegeCampAttackInfo;

import com.flansmod.warforge.common.faction.ClaimType;

import net.minecraft.resources.ResourceLocation;

public class ClaimChunkRenderInfo extends SiegeCampAttackInfoRender {
    public final ClaimType claimType;
    public final boolean forceLoaded;
    public final boolean battleZone;

    public ClaimChunkRenderInfo(SiegeCampAttackInfo info, ClaimType claimType, boolean forceLoaded, boolean conquered, boolean battleZone, ResourceLocation centerIcon, CenterMarkType centerIconType) {
        super(info);
        this.claimType = claimType;
        this.forceLoaded = forceLoaded;
        this.conquered = conquered;
        this.battleZone = battleZone;
        if (info instanceof SiegeCampAttackInfoRender renderInfo) {
            setCenterMarkType(renderInfo.getCenterMarkType());
            setCenterIcon(renderInfo.getCenterIcon());
            setVeinIcon(renderInfo.getVeinIcon());
        }
        if (claimType == ClaimType.SIEGE && getCenterMarkType() == CenterMarkType.NONE) {
            setCenterMarkType(CenterMarkType.SIEGE_CAMP);
        }
        if (centerIcon != null) {
            setCenterMarkType(centerIconType);
            setCenterIcon(centerIcon);
        }
    }
}
