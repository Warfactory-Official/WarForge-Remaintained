package com.flansmod.warforge.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.Deque;

public class SyncQueueHandler {
    public static class SyncTask {
        public final EntityPlayerMP player;
        public final Runnable runnable;

        public SyncTask(EntityPlayerMP player, Runnable runnable) {
            this.player = player;
            this.runnable = runnable;
        }
    }

    private static final Deque<SyncTask> syncTasks = new ArrayDeque<>();
    public static final int perTick  = 4;

    public static void enqueue(EntityPlayerMP player, Runnable task) {
        if (player == null || player.hasDisconnected()) return;
        syncTasks.add(new SyncTask(player, task));
    }

    public static void sync(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !syncTasks.isEmpty()) {
            for (int i = 0; i < perTick && !syncTasks.isEmpty(); i++) {
                SyncTask task = syncTasks.poll();
                if (task.player != null && task.player.hasDisconnected()) {
                    syncTasks.removeIf(t -> t.player == task.player);
                } else if (task.runnable != null) {
                    task.runnable.run();
                }
            }
        }
    }
}
