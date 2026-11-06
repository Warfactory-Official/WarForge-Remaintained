package com.flansmod.warforge.client.render;

import com.flansmod.warforge.client.ClientConfig;
import com.flansmod.warforge.common.WarForgeMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Flashback reports spectator mode to hide the vanilla HUD, which GUI-event mods do not consult. Optional: absent => not a replay. */
final class FlashbackReplay {
    private static final Object[] NO_ARGS = {};
    private static final Method IN_REPLAY = resolve("isInReplay");
    private static final Method VISUAL_MILLIS = resolve("getVisualMillis");
    private static final Method REPLAY_SERVER = resolve("getReplayServer");

    private FlashbackReplay() {}

    static boolean active() {
        return IN_REPLAY != null && (Boolean) invoke(IN_REPLAY);
    }

    /** The WarForge HUD is hidden in replays unless the client config opts in (footage capture). */
    static boolean hidesHud() {
        return !ClientConfig.SHOW_HUD_IN_REPLAYS && active();
    }

    /** Replay tick * 50 (export-frame tick while exporting); only meaningful while {@link #active}. */
    static long visualMillis() {
        return VISUAL_MILLIS == null ? System.currentTimeMillis() : (Long) invoke(VISUAL_MILLIS);
    }

    /** Identity of the open replay. */
    static Object server() {
        return REPLAY_SERVER == null ? null : invoke(REPLAY_SERVER);
    }

    private static Object invoke(Method method) {
        try {
            return method.invoke(null, NO_ARGS);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Flashback." + method.getName() + " failed", e);
        }
    }

    private static Method resolve(String name) {
        try {
            return Class.forName("com.moulberry.flashback.Flashback").getMethod(name);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException | LinkageError e) {
            WarForgeMod.LOGGER.debug("Flashback present without {}", name, e);
            return null;
        }
    }
}
