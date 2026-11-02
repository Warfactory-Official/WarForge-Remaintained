package com.flansmod.warforge.client;

import com.flansmod.warforge.compat.ClientCompat;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class DeferredGuiOpen {
    private static Runnable pending;
    private static Runnable parentReopen;
    private static double restoreMouseX;
    private static double restoreMouseY;
    private static boolean restoreMouse;

    private DeferredGuiOpen() {
    }

    public static void open(Runnable opener) {
        parentReopen = null;
        schedule(opener);
    }

    public static void openChild(Runnable reopenParent, Runnable opener) {
        parentReopen = reopenParent;
        schedule(opener);
    }

    public static void openSibling(Runnable opener) {
        schedule(opener);
    }

    public static void closeOrReturnToParent() {
        Runnable reopenParent = parentReopen;
        parentReopen = null;
        if (reopenParent != null) {
            reopenParent.run();
        } else {
            ClientCompat.setScreen(null);
        }
    }

    public static void tick() {
        Runnable opener = pending;
        if (opener == null) {
            return;
        }
        pending = null;
        opener.run();
    }

    public static void onScreenOpened() {
        if (!restoreMouse) {
            return;
        }
        restoreMouse = false;
        GLFW.glfwSetCursorPos(ClientCompat.windowHandle(), restoreMouseX, restoreMouseY);
    }

    private static void schedule(Runnable opener) {
        Minecraft mc = Minecraft.getInstance();
        restoreMouseX = mc.mouseHandler.xpos();
        restoreMouseY = mc.mouseHandler.ypos();
        restoreMouse = true;
        // Server must return to the inventory menu before the child opens.
        ClientCompat.setScreen(null);
        pending = opener;
    }
}
