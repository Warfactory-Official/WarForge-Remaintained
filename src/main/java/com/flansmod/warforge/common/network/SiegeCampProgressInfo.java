package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimBlockPos;

import java.util.UUID;

public class SiegeCampProgressInfo {
    public static final int STALE_TICKS = 400;
    public static final int FINISHED_LINGER_TICKS = 100;

    public DimBlockPos defendingPos;
    public DimBlockPos attackingPos;
    public int attackingColour;
    public int defendingColour;
    public String attackingName;
    public String defendingName;
    public int battleRadius = 1;
    public int siegedRadius = 0;
    public UUID defendingFactionId = FactionIds.NONE;
    public UUID attackingFactionId = FactionIds.NONE;
    /** Seconds until the attacking siege is abandoned; 0 = not at risk. */
    public int attackerAbandonSeconds = 0;

    public int completionPoint = 5;
    public int previousProgress = 0;
    public int progress = 0;
    public long timeProgress = 0;
    public long endTimestamp = Long.MAX_VALUE;

    public int expiredTicks = 0;
    public boolean finished = false;
    public int staleTicks = 0;

    public void clientTick() {
        staleTicks++;
        if (finished) {
            expiredTicks++;
        }
    }

    public boolean isExpired() {
        return expiredTicks >= FINISHED_LINGER_TICKS || staleTicks >= STALE_TICKS;
    }
}
