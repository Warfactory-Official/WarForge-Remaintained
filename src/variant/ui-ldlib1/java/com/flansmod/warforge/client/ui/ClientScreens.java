package com.flansmod.warforge.client.ui;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientScreens {
    private ClientScreens() {
    }

    public static ModularUIGuiContainer open(WidgetGroup root) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
            mc.player.closeContainer();
        }
        root.setClientSideWidget();
        ModularUI ui = new ModularUI(root, IUIHolder.EMPTY, mc.player);
        ui.initWidgets();
        ModularUIGuiContainer screen = new ModularUIGuiContainer(ui, 0);
        mc.setScreen(screen);
        return screen;
    }

    public static void close() {
        Minecraft.getInstance().setScreen(null);
    }
}
