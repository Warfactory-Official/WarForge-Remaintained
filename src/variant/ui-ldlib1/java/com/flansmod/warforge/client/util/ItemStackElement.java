package com.flansmod.warforge.client.util;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ItemStackElement extends Widget {
    private final ItemStack stack;
    private final boolean decorations;
    private @Nullable List<Component> tooltip;

    public ItemStackElement(ItemStack stack, int x, int y, int size, boolean slotBackground, boolean decorations) {
        super(x, y, size, size);
        this.stack = stack;
        this.decorations = decorations;
        if (slotBackground) {
            setBackground(SlotWidget.ITEM_SLOT_TEXTURE);
        }
    }

    @Override
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        if (stack.isEmpty()) {
            return;
        }
        int x = getPositionX() + (getSizeWidth() - 16) / 2;
        int y = getPositionY() + (getSizeHeight() - 16) / 2;
        graphics.renderItem(stack, x, y);
        if (decorations) {
            graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
        }
    }

    @Override
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        if (!stack.isEmpty() && isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this
                && gui != null && gui.getModularUIGui() != null) {
            if (tooltip == null) {
                tooltip = Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
            }
            gui.getModularUIGui().setHoverTooltip(tooltip, stack, null, null);
        }
    }
}
