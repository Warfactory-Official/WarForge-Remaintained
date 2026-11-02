package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.ClaimType;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.core.Vec3i;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
public class SiegeCampAttackInfo
{
    public boolean canAttack;
    public Vec3i offset;  //more flexible than DirectionFacing, Y value is ignored
    public UUID factionUuid;
    public String factionName;
    public int factionColour;
    public Vein warforgeVein;
    public Quality oreQuality;
    public ClaimType claimType = ClaimType.NONE;
    public byte momentum;
    public boolean conquered;

    public SiegeCampAttackInfo(SiegeCampAttackInfo info) {
        this(
                info.canAttack,
                info.offset,
                info.factionUuid,
                info.factionName,
                info.factionColour,
                info.warforgeVein,
                info.oreQuality,
                info.claimType,
                info.momentum,
                info.conquered
        );
    }
}
