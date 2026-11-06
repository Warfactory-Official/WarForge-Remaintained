package com.flansmod.warforge.client.render;

import java.lang.ref.WeakReference;

/**
 * HUD time. Live: the wall clock. In a Flashback replay: replay time ({@code Flashback.getVisualMillis}, which also
 * follows export frames), so HUD timers hold while paused and follow seeks and exports.
 */
public final class HudClock {
    private static WeakReference<Object> offsetReplay = new WeakReference<>(null);
    private static long offset;

    private HudClock() {}

    public static boolean replay() {
        return FlashbackReplay.active();
    }

    public static long now() {
        return replay() ? FlashbackReplay.visualMillis() : System.currentTimeMillis();
    }

    /** A server deadline ({@code deadline}, sent at {@code serverNow}) on this clock. */
    public static long rebase(long deadline, long serverNow) {
        if (!replay()) {
            return System.currentTimeMillis() + (deadline - serverNow);
        }
        Object replay = FlashbackReplay.server();
        if (offsetReplay.get() != replay) {
            offsetReplay = new WeakReference<>(replay);
            offset = Long.MIN_VALUE;
        }
        // A seek re-delivers earlier packets late, never early: the largest server-minus-replay offset is the true one.
        offset = Math.max(offset, serverNow - FlashbackReplay.visualMillis());
        return deadline - offset;
    }
}
