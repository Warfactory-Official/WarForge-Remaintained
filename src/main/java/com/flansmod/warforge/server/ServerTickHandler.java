package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;

import com.flansmod.warforge.common.network.SyncQueueHandler;

public class ServerTickHandler
{
	public void OnTick() {
		WarForgeMod.gameTimeMs += 50L;
		WarForgeMod.INSTANCE.updateServer();
		WarForgeMod.NETWORK.handleServerPackets();
		WarForgeMod.PROTECTIONS.UpdateServer();
		WarForgeMod.TELEPORTS.update();
		WarForgeMod.FOBS.getWarpQueue().update();
		WarForgeMod.FOBS.processCleanupQueue();
		WarForgeMod.proxy.TickServer();
		WarForgeMod.FACTIONS.update();
		WarForgeMod.JOURNEYMAP_SYNC.tick();
		WarForgeMod.JOURNEYMAP_VEIN_SYNC.tick();
		SyncQueueHandler.sync();


		// WarForgeMod.LOGGER.info("Current tick: " + WarForgeMod.ServerTick);
	}
}
