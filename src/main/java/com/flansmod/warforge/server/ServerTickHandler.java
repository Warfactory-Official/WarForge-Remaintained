package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.SyncQueueHandler;

public class ServerTickHandler {
    public void onTick() {
        if (WarForgeMod.isReplayServer()) {
            return;
        }
        WarClock.tick();
        WarForgeMod.TELEPORTS.update();
        WarForgeMod.FOBS.getWarpQueue().update();
        WarForgeMod.FOBS.processCleanupQueue();
        WarForgeMod.proxy.tickServer();
        WarForgeMod.FACTIONS.update();
        WarForgeMod.JOURNEYMAP_VEIN_SYNC.tick();
        SyncQueueHandler.sync();
    }
}
