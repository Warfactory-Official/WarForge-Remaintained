package com.flansmod.warforge.compat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Client accessors whose names differ between versions (client only). */
public final class ClientCompat {
    private ClientCompat() {}

    /** The chat screen with the given text already typed. */
    public static ChatScreen chatScreen(String initial) {
        return new ChatScreen(initial, false);
    }

    public static Vec3 cameraPosition(Camera camera) {
        return camera.position();
    }

    /** Queues a task for the client thread (always deferred, even when called on it). */
    public static void runLater(Runnable task) {
        Minecraft.getInstance().schedule(task);
    }

    /** The GLFW handle of the game window. */
    public static long windowHandle() {
        return Minecraft.getInstance().getWindow().handle();
    }

    /** Shows a message above the hotbar. */
    public static void overlayMessage(Player player, Component message) {
        player.sendOverlayMessage(message);
    }

    /** Opens the screen (null closes the current one). */
    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    /** The open screen, or null. */
    public static @Nullable Screen screen() {
        return Minecraft.getInstance().screen;
    }

    public static Camera mainCamera() {
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    /** The packed block and sky light at a position (26.x builds only). */
    public static int lightCoords(Level level, BlockPos pos) {
        return LevelRenderer.getLightCoords(level, pos);
    }

    /** The toast manager (26.x builds only). */
    public static ToastManager toastManager() {
        return Minecraft.getInstance().getToastManager();
    }
}
