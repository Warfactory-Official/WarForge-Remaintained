package com.flansmod.warforge.client.render;

import com.flansmod.warforge.compat.ClientCompat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.jetbrains.annotations.Nullable;

/** Base class of WarForge toasts. The vanilla toast API differs between versions. */
public abstract class WFToast implements Toast {
    private Visibility visibility = Visibility.SHOW;

    /** Draws the toast at (0, 0). timeMs counts from when the toast system started showing it. */
    protected abstract void draw(Gui2D graphics, Font font, long timeMs);

    /** True while the toast should stay on screen. */
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

    /** The toast of this type and token that is showing or queued, or null. */
    public static <T extends WFToast> @Nullable T find(Class<T> type, Object token) {
        return ClientCompat.toastManager().getToast(type, token);
    }

    public static void add(WFToast toast) {
        ClientCompat.toastManager().addToast(toast);
    }
}
