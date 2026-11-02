package com.flansmod.warforge.client;

import com.flansmod.warforge.platform.Platform;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

public final class ClientGameEvents {
    private ClientGameEvents() {}

    public static void onTickEnd(EventPriority priority, Runnable handler) {
        Platform.gameBus().addListener(priority, (TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) handler.run();
        });
    }
}
