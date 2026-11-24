package com.flansmod.warforge.common.util;

import com.flansmod.warforge.common.WarForgeMod;

import java.util.concurrent.ConcurrentLinkedQueue;

/** Work only the server thread may do (claim/faction/siege/fob mutation, chunk tickets); region workers enqueue. */
public final class MainThread {
    private static final ConcurrentLinkedQueue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();

    private MainThread() {
    }

    public static boolean on() {
        return WarForgeMod.MC_SERVER.isSameThread();
    }

    /** Inline on the server thread; otherwise drained at the end of the next server tick. */
    public static void run(Runnable op) {
        if (on()) {
            op.run();
        } else {
            QUEUE.add(op);
        }
    }

    /** Server thread, ServerTickEvent.Post. Op failure must not drop the rest. */
    public static void drain() {
        Runnable op;
        while ((op = QUEUE.poll()) != null) {
            try {
                op.run();
            } catch (RuntimeException e) {
                WarForgeMod.LOGGER.error("Deferred main-thread op failed", e);
            }
        }
    }

    public static void clear() {
        QUEUE.clear();
    }
}
