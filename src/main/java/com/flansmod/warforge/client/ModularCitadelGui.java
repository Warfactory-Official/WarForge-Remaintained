package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.flansmod.warforge.common.CommonProxy;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import com.flansmod.warforge.common.factories.FactionInsuranceGuiFactory;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiFactory;
import com.flansmod.warforge.common.factories.FactionStatsGuiFactory;
import com.flansmod.warforge.common.factories.FactionUpgradeGuiFactory;
import com.flansmod.warforge.common.network.PacketDisbandFaction;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemShield;
import net.minecraftforge.items.wrapper.InvWrapper;

public final class ModularCitadelGui {
    private static final int PANEL_WIDTH = 350;
    private static final int PANEL_HEIGHT = 250;
    private static final int SLOT_SIZE = 18;
    private static final int CONTENT_LEFT = 12;
    private static final int HEADER_Y = 12;
    private static final int STORAGE_Y = 54;
    private static final int ACTIONS_Y = 130;
    private static final int INVENTORY_Y = 168;

    private ModularCitadelGui() {
    }

    public static ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings, TileEntityCitadel citadel) {
        syncManager.bindPlayerInventory(data.getPlayer());
        InvWrapper handler = new InvWrapper(citadel);
        boolean hasFaction = !citadel.getFaction().equals(Faction.nullUuid);

        ModularPanel panel = ModularPanel.defaultPanel("citadel_modular")
                .width(PANEL_WIDTH)
                .height(PANEL_HEIGHT)
                .topRel(0.40f);

        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(0, 0, PANEL_WIDTH, 36, 0xFF171B1F, 0xFF0D1013)).size(PANEL_WIDTH, 36));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, STORAGE_Y, 196, 68, 0xEE20262B, 0xEE11161A)).size(196, 68).pos(CONTENT_LEFT, STORAGE_Y));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(220, STORAGE_Y, 118, 68, 0xEE20262B, 0xEE11161A)).size(118, 68).pos(220, STORAGE_Y));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, ACTIONS_Y, PANEL_WIDTH - CONTENT_LEFT * 2, 52, 0xEE20262B, 0xEE11161A)).size(PANEL_WIDTH - CONTENT_LEFT * 2, 52).pos(CONTENT_LEFT, ACTIONS_Y));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, INVENTORY_Y, PANEL_WIDTH - CONTENT_LEFT * 2, 70, 0xEE20262B, 0xEE11161A)).size(PANEL_WIDTH - CONTENT_LEFT * 2, 70).pos(CONTENT_LEFT, INVENTORY_Y));

        panel.child(new IDrawable.DrawableWidget(colorStripe(citadel.colour, 0, 0, 6, PANEL_HEIGHT)).size(6, PANEL_HEIGHT));
        panel.child(ButtonWidget.panelCloseButton().pos(PANEL_WIDTH - 18, 8));

        panel.child(IKey.str(hasFaction ? citadel.getClaimDisplayName() : "Unclaimed Citadel").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y)
                .style(net.minecraft.util.text.TextFormatting.BOLD)
                .color(hasFaction ? citadel.colour : 0xFFFFFF)
                .shadow(true)
                .scale(1.2f));
        panel.child(IKey.str(hasFaction ? "Faction vault, banner relay, and command center" : "Claimed by the placer until a faction is founded").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y + 14)
                .color(0xC7CCD1));

        panel.child(IKey.str("Yield Storage").asWidget()
                .pos(CONTENT_LEFT + 8, STORAGE_Y + 6)
                .style(net.minecraft.util.text.TextFormatting.BOLD));
        panel.child(IKey.str("Resources collected from claimed territory").asWidget()
                .pos(CONTENT_LEFT + 8, STORAGE_Y + 18)
                .color(0xB8BDC3));

        Grid yieldGrid = new Grid()
                .mapTo(3, TileEntityCitadel.NUM_YIELD_STACKS, slot ->
                        new ItemSlot()
                                .slot(new ModularSlot(handler, slot))
                                .size(SLOT_SIZE))
                .size(3 * SLOT_SIZE, 3 * SLOT_SIZE)
                .pos(CONTENT_LEFT + 8, STORAGE_Y + 32);
        panel.child(yieldGrid);

        panel.child(IKey.str("Banner Relay").asWidget()
                .pos(228, STORAGE_Y + 6)
                .style(net.minecraft.util.text.TextFormatting.BOLD));
        panel.child(IKey.str("Sets the faction banner copied into owned land").asWidget()
                .pos(228, STORAGE_Y + 18)
                .color(0xB8BDC3));

        ItemSlot bannerSlot = new ItemSlot()
                .slot(new ModularSlot(handler, TileEntityCitadel.BANNER_SLOT_INDEX)
                        .filter(stack -> stack.getItem() instanceof ItemBanner || stack.getItem() instanceof ItemShield))
                .size(SLOT_SIZE)
                .pos(228, STORAGE_Y + 38);
        panel.child(bannerSlot);
        panel.child(IKey.str("Banner or shield").asWidget()
                .pos(252, STORAGE_Y + 42)
                .color(0xD6DBE0));

        panel.child(IKey.str("Command Surface").asWidget()
                .pos(CONTENT_LEFT + 8, ACTIONS_Y + 6)
                .style(net.minecraft.util.text.TextFormatting.BOLD));

        if (hasFaction) {
            panel.child(openButton("Insurance", CONTENT_LEFT + 8, ACTIONS_Y + 16, 72, () -> FactionInsuranceGuiFactory.INSTANCE.openClient(citadel.getFaction())));
            panel.child(openButton("Faction Stats", CONTENT_LEFT + 84, ACTIONS_Y + 16, 86, () -> FactionStatsGuiFactory.INSTANCE.openClient(citadel.getFaction())));
            panel.child(openButton("Members", CONTENT_LEFT + 174, ACTIONS_Y + 16, 60, () -> FactionMemberManagerGuiFactory.INSTANCE.openClient(FactionMemberManagerGuiData.Page.MEMBERS)));
            if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
                panel.child(openButton("Upgrade", CONTENT_LEFT + 238, ACTIONS_Y + 16, 54, () -> FactionUpgradeGuiFactory.INSTANCE.openClient(citadel.getFaction())));
            }
            panel.child(openButton("Recolor", CONTENT_LEFT + 296, ACTIONS_Y + 16, 42, () -> Minecraft.getMinecraft().player.openGui(
                    WarForgeMod.INSTANCE,
                    CommonProxy.GUI_TYPE_RECOLOUR_FACTION,
                    Minecraft.getMinecraft().world,
                    citadel.getPos().getX(),
                    citadel.getPos().getY(),
                    citadel.getPos().getZ()
            )));
            panel.child(dangerButton("Disband", CONTENT_LEFT + 8, ACTIONS_Y + 36, 54));
        } else {
            panel.child(openButton("Create Faction", CONTENT_LEFT + 8, ACTIONS_Y + 16, 100, () -> Minecraft.getMinecraft().player.openGui(
                    WarForgeMod.INSTANCE,
                    CommonProxy.GUI_TYPE_CREATE_FACTION,
                    Minecraft.getMinecraft().world,
                    citadel.getPos().getX(),
                    citadel.getPos().getY(),
                    citadel.getPos().getZ()
            )));
            panel.child(IKey.str("The placer can establish the faction from here.").asWidget()
                    .pos(CONTENT_LEFT + 116, ACTIONS_Y + 21)
                    .color(0xB8BDC3));
        }

        panel.child(IKey.str("Inventory").asWidget()
                .pos(CONTENT_LEFT + 8, INVENTORY_Y + 6)
                .style(net.minecraft.util.text.TextFormatting.BOLD));
        panel.child(SlotGroupWidget.playerInventory(false)
                .pos(CONTENT_LEFT + 8, INVENTORY_Y + 20));

        return panel;
    }

    private static IDrawable sectionBackdrop(int x, int y, int width, int height, int fillColor, int borderColor) {
        return new IDrawable() {
            @Override
            public void draw(GuiContext context, int drawX, int drawY, int drawWidth, int drawHeight, WidgetTheme theme) {
                Gui.drawRect(drawX, drawY, drawX + width, drawY + height, fillColor);
                Gui.drawRect(drawX, drawY, drawX + width, drawY + 1, borderColor);
                Gui.drawRect(drawX, drawY + height - 1, drawX + width, drawY + height, borderColor);
                Gui.drawRect(drawX, drawY, drawX + 1, drawY + height, borderColor);
                Gui.drawRect(drawX + width - 1, drawY, drawX + width, drawY + height, borderColor);
            }
        };
    }

    private static IDrawable colorStripe(int color, int x, int y, int width, int height) {
        return new IDrawable() {
            @Override
            public void draw(GuiContext context, int drawX, int drawY, int drawWidth, int drawHeight, WidgetTheme theme) {
                Gui.drawRect(drawX, drawY, drawX + width, drawY + height, 0xFF000000 | (color & 0x00FFFFFF));
            }
        };
    }

    private static ButtonWidget<?> openButton(String label, int x, int y, int width, Runnable action) {
        return new ButtonWidget<>()
                .width(width)
                .height(18)
                .overlay(IKey.str(label))
                .background(GuiTextures.MC_BUTTON)
                .hoverBackground(GuiTextures.MC_BUTTON_HOVERED)
                .onMousePressed(mouseButton -> {
                    action.run();
                    return true;
                })
                .pos(x, y);
    }

    private static ButtonWidget<?> dangerButton(String label, int x, int y, int width) {
        return new ButtonWidget<>()
                .width(width)
                .height(18)
                .overlay(IKey.str(label).color(0xFFEAEA))
                .background(buttonFill(0xFF7A2D2D))
                .hoverBackground(buttonFill(0xFF944040))
                .onMousePressed(mouseButton -> {
                    WarForgeMod.NETWORK.sendToServer(new PacketDisbandFaction());
                    return true;
                })
                .pos(x, y);
    }

    private static IDrawable buttonFill(int color) {
        return (context, x, y, width, height, theme) -> {
            Gui.drawRect(x, y, x + width, y + height, color);
            Gui.drawRect(x, y, x + width, y + 1, 0xFF0F0F0F);
            Gui.drawRect(x, y + height - 1, x + width, y + height, 0xFF0F0F0F);
            Gui.drawRect(x, y, x + 1, y + height, 0xFF0F0F0F);
            Gui.drawRect(x + width - 1, y, x + width, y + height, 0xFF0F0F0F);
        };
    }
}
