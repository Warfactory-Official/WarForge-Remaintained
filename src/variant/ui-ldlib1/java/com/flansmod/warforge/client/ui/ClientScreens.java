package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.compat.carryon.CarryOnCompat;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class ClientScreens {
    private ClientScreens() {
    }

    /** Null when refused: Carry On cancels the init of every container screen while carrying and leaves the half-opened one behind. */
    public static @Nullable ModularUIGuiContainer open(WidgetGroup root) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && CarryOnCompat.isCarrying(mc.player)) {
            ClientCompat.overlayMessage(mc.player, Component.translatable("warforge.client.carryon.blocked"));
            return null;
        }
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
