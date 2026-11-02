package com.flansmod.warforge.client;

import com.flansmod.warforge.platform.Platform;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ClientGameEvents {
    private ClientGameEvents() {}

    public static void onTickEnd(EventPriority priority, Runnable handler) {
        Platform.gameBus().addListener(priority, (ClientTickEvent.Post event) -> handler.run());
    }
}
