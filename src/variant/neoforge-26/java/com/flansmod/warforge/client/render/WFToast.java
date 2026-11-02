package com.flansmod.warforge.client.render;

import com.flansmod.warforge.compat.ClientCompat26;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.jetbrains.annotations.Nullable;

public abstract class WFToast implements Toast {
    private Visibility visibility = Visibility.SHOW;

    protected abstract void draw(Gui2D graphics, Font font, long timeMs);

    protected abstract boolean isVisible(long timeMs);

    @Override
    public final Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public final void update(ToastManager manager, long fullyVisibleForMs) {
        visibility = isVisible(fullyVisibleForMs) ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        draw(Gui2D.of(graphics), font, fullyVisibleForMs);
    }

    public static <T extends WFToast> @Nullable T find(Class<T> type, Object token) {
        return ClientCompat26.toastManager().getToast(type, token);
    }

    public static void add(WFToast toast) {
        ClientCompat26.toastManager().addToast(toast);
    }
}
