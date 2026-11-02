package com.flansmod.warforge.client.render;

import com.flansmod.warforge.common.WarForgeMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Flashback reports spectator mode to hide the vanilla HUD, which GUI-event mods do not consult. Optional: absent => not a replay. */
final class FlashbackReplay {
    private static final Object[] NO_ARGS = {};
    private static final Method IN_REPLAY = resolve();

    private FlashbackReplay() {}

    static boolean active() {
        if (IN_REPLAY == null) {
            return false;
        }
        try {
            return (Boolean) IN_REPLAY.invoke(null, NO_ARGS);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Flashback.isInReplay failed", e);
        }
    }

    private static Method resolve() {
        try {
            return Class.forName("com.moulberry.flashback.Flashback").getMethod("isInReplay");
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException | LinkageError e) {
            WarForgeMod.LOGGER.debug("Flashback present without isInReplay", e);
            return null;
        }
    }
}
