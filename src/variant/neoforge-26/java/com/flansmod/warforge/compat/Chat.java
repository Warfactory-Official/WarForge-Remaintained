package com.flansmod.warforge.compat;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Chat component events. They became records per action in 1.21.5. */
public final class Chat {
    private Chat() {}

    public static ClickEvent runCommand(String command) {
        return new ClickEvent.RunCommand(command);
    }

    public static HoverEvent showText(Component text) {
        return new HoverEvent.ShowText(text);
    }
}
