package com.flansmod.warforge.client;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.client.render.BlockEntityRenderers;
import com.flansmod.warforge.client.render.ClientRenderEvents;
import com.flansmod.warforge.client.render.KeyMaps;
import com.flansmod.warforge.platform.Platform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class WarForgeClientInit {
    private WarForgeClientInit() {
    }

    public static void init(IEventBus modBus) {
        ClientPackets.register();
        modBus.addListener((ModConfigEvent event) -> {
            if (event.getConfig().getSpec() == ClientConfig.SPEC) {
                ClientConfig.bake();
            }
        });
        WarForgeMod.proxy = new ClientProxy();
        modBus.register(new ModelEventHandler());
        modBus.register(ClientContent.class);
        Platform.gameBus().register(WarForgeClientEventHandler.class);
        ClientProxy proxy = (ClientProxy) WarForgeMod.proxy;
        modBus.addListener(proxy::clientSetup);
        BlockEntityRenderers.register(modBus);
        ClientRenderEvents.init(modBus);
        ClientRenderEvents.onPlayerNameTag(WarForgeClientEventHandler::nameplateFor);
        KeyMaps.register(modBus, ClientProxy.FACTION_CHAT_KEY, ClientKeybinds.TOGGLE_BORDERS,
                ClientKeybinds.CLAIM_MANAGER, ClientKeybinds.TOGGLE_VEIN_OVERLAY);
    }
}
