package com.flansmod.warforge.compat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
        return new ChatScreen(initial);
    }

    public static Vec3 cameraPosition(Camera camera) {
        return camera.getPosition();
    }

    /** Queues a task for the client thread (always deferred, even when called on it). */
    public static void runLater(Runnable task) {
        Minecraft.getInstance().tell(task);
    }

    /** The GLFW handle of the game window. */
    public static long windowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    /** Shows a message above the hotbar. */
    public static void overlayMessage(Player player, Component message) {
        player.displayClientMessage(message, true);
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
}
