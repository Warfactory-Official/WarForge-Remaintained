package com.flansmod.warforge.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;

public final class ClientCompat26 {
    private ClientCompat26() {}

    public static int lightCoords(Level level, BlockPos pos) {
        return LightCoordsUtil.getLightCoords(level, pos);
    }

    public static ToastManager toastManager() {
        return Minecraft.getInstance().gui.toastManager();
    }
}
