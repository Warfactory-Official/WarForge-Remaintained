package com.flansmod.warforge.compat;

import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ClientCompat {
    private ClientCompat() {}

    public static ChatScreen chatScreen(String initial) {
        return new ChatScreen(initial);
    }

    public static Vec3 cameraPosition(Camera camera) {
        return camera.getPosition();
    }

    public static void runLater(Runnable task) {
        Minecraft.getInstance().tell(task);
    }

    public static long windowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    public static void overlayMessage(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    public static @Nullable Screen screen() {
        return Minecraft.getInstance().screen;
    }

    public static Camera mainCamera() {
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    public static boolean fastGraphics() {
        return Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
    }
}
