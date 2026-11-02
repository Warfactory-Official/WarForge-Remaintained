package com.flansmod.warforge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.jetbrains.annotations.Nullable;

public abstract class WFToast implements Toast {
    protected abstract void draw(Gui2D graphics, Font font, long timeMs);

    protected abstract boolean isVisible(long timeMs);

    @Override
    public final Visibility render(GuiGraphics graphics, ToastComponent component, long timeMs) {
        draw(Gui2D.of(graphics), component.getMinecraft().font, timeMs);
        return isVisible(timeMs) ? Visibility.SHOW : Visibility.HIDE;
    }

    public static <T extends WFToast> @Nullable T find(Class<T> type, Object token) {
        return Minecraft.getInstance().getToasts().getToast(type, token);
    }

    public static void add(WFToast toast) {
        Minecraft.getInstance().getToasts().addToast(toast);
    }
}
