package com.flansmod.warforge.client;

import com.flansmod.warforge.common.ModelEventHandler;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.client.render.BlockEntityRenderers;
import com.flansmod.warforge.client.render.ClientRenderEvents;
import com.flansmod.warforge.client.render.KeyMaps;
import com.flansmod.warforge.platform.Platform;
import net.neoforged.bus.api.IEventBus;

public final class WarForgeClientInit {
    private WarForgeClientInit() {
    }

    public static void init(IEventBus modBus) {
        WarForgeMod.NAMETAG_CACHE = new PlayerNametagCache(60_000, 200);
        modBus.register(new ModelEventHandler());
        modBus.register(ClientContent.class);
        Platform.gameBus().register(WarForgeClientEventHandler.class);
        ClientProxy proxy = (ClientProxy) WarForgeMod.proxy;
        modBus.addListener(proxy::clientSetup);
        BlockEntityRenderers.register(modBus);
        ClientRenderEvents.init(modBus);
        ClientRenderEvents.onPlayerNameTag(WarForgeClientEventHandler::nameplateFor);
        KeyMaps.register(modBus, ClientProxy.factionChatKey, ClientTickHandler.toggleBordersKey,
                ClientTickHandler.claimManagerKey, ClientTickHandler.toggleVeinOverlayKey);
    }
}
