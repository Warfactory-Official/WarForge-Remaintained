package com.flansmod.warforge.client;

import com.flansmod.warforge.client.render.ClientRenderEvents;
import com.flansmod.warforge.client.render.KeyMaps;
import com.flansmod.warforge.common.CommonProxy;
import com.flansmod.warforge.client.fx.AnimatedEffectHandler;
import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.platform.Platform;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public class ClientProxy extends CommonProxy {
    public static final ChunkVeinCache CHUNK_VEIN_CACHE = new ChunkVeinCache();

    public static final KeyMapping FACTION_CHAT_KEY = KeyMaps.create("key.factionchat.desc", GLFW.GLFW_KEY_Y, "key.warforge.factionchat");

    public void clientSetup(FMLClientSetupEvent event) {
        ClientTickHandler clientTickHandler = new ClientTickHandler();
        Platform.gameBus().register(clientTickHandler);
        ClientGameEvents.onTickEnd(EventPriority.NORMAL, clientTickHandler::onTick);
        ClientRenderEvents.onAfterTranslucentBlocks(clientTickHandler::onRenderLast);
        ClientRenderEvents.onHud(clientTickHandler::onRenderHUD);
        if (Platform.isDevelopment()) {
            Platform.gameBus().register(new SiegeUiDebugCommand());
        }
        ClientGameEvents.onTickEnd(EventPriority.LOWEST, new AnimatedEffectHandler()::onClientTick);
        Platform.gameBus().register(new ClientMineTimePredictor());
        Platform.gameBus().register(new ClientPlacementPredictor());
    }

    @Override
    public void tickClient() {
        if (FACTION_CHAT_KEY.consumeClick()) {
            ClientCompat.setScreen(ClientCompat.chatScreen("/f chat "));
        }
    }
}
