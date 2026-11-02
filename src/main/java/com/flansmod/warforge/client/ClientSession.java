package com.flansmod.warforge.client;

import com.flansmod.warforge.client.ui.model.ClaimMapModel;
import com.flansmod.warforge.common.SyncedVeins;
import com.flansmod.warforge.client.border.ClaimBorderRenderer;
import com.flansmod.warforge.client.hud.AreaToastHud;
import com.flansmod.warforge.client.hud.TimersHud;
import com.flansmod.warforge.client.hud.VeinHud;
import com.flansmod.warforge.client.map.ChunkMapTextureDaemon;
import com.flansmod.warforge.client.render.MapCellPainter;
import com.flansmod.warforge.client.util.LayeredItemIconCache;
import com.flansmod.warforge.compat.ClientCompat;

public final class ClientSession {
    private ClientSession() {
    }

    public static void reset() {
        ClaimBorderRenderer.reset();
        BorderSyncScheduler.reset();
        ClientClaimChunkCache.clear();
        ClientSiegeState.clear();
        SiegeUiDebugCommand.reset();
        TimersHud.reset();
        AreaToastHud.reset();
        VeinHud.reset();

        ClientProxy.CHUNK_VEIN_CACHE.purge();
        SyncedVeins.ENTRIES.clear();
        PlayerNametagCache.INSTANCE.purge();
        ServerTerrainCache.clear();
        ClaimMapModel.resetSiegeState();
        ClientBorderCache.clear();
        JourneyMapVeinCache.applyClear();

        ClientFlagRegistry.clear();
        ClientUpgradeTable.clear();
        LayeredItemIconCache.clear();
        MapCellPainter.clearTextureCache();
        ClientCompat.runLater(ChunkMapTextureDaemon::releaseAll);
    }
}
