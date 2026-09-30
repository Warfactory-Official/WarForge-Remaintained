package com.flansmod.warforge.client.ui;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Opens client-only LDLib1 screens (no server container): the widget tree is marked client-side so no widget
 * sends actions to the server, and it is shown in a ModularUIGuiContainer with a detached menu. The root's size is
 * the UI size; LDLib centres it on the screen.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientScreens {
    private ClientScreens() {
    }

    /** Wraps root (already laid out) in a client-side ModularUI and shows it. Returns the opened screen. */
    public static ModularUIGuiContainer open(WidgetGroup root) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
            // A server container (a block UI) is open: close it properly so the server and client agree, since
            // client screens are later closed without a container close packet.
            mc.player.closeContainer();
        }
        root.setClientSideWidget();
        ModularUI ui = new ModularUI(root, IUIHolder.EMPTY, mc.player);
        ui.initWidgets();
        ModularUIGuiContainer screen = new ModularUIGuiContainer(ui, 0);
        mc.setScreen(screen);
        return screen;
    }

    /** Closes the current screen. */
    public static void close() {
        Minecraft.getInstance().setScreen(null);
    }
}
