package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.RichTextWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.flansmod.warforge.client.util.FlagDrawable;
import com.flansmod.warforge.common.CommonProxy;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import com.flansmod.warforge.common.factories.*;
import com.flansmod.warforge.common.network.PacketDisbandFaction;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.wrapper.InvWrapper;

public final class ModularCitadelGui {
    private static final int PANEL_WIDTH = 350;
    private static final int PANEL_HEIGHT = 200;
    private static final int SLOT_SIZE = 18;
    private static final int CONTENT_LEFT = 12;
    private static final int HEADER_Y = 12;
    private static final int STORAGE_Y = 40;
    private static final int ACTIONS_Y = 130;

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
        var yeldPanel = new Flow(GuiAxis.Y)
                .background(sectionBackdrop(100, 80, 0xEE20262B, 0xEE11161A))
                .size(100, 80)
                .pos(CONTENT_LEFT, STORAGE_Y)
                .padding(5)
                .margin(5);
        panel.child(yeldPanel);

        var flagPanel = new Flow(GuiAxis.Y)
                .background((sectionBackdrop(215, 80, 0xEE20262B, 0xEE11161A)))
                .size(210, 80)
                .pos(CONTENT_LEFT + 110, STORAGE_Y)
                .padding(5)
                .margin(5);

        panel.child(flagPanel);

        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(PANEL_WIDTH, 36, 0xFF171B1F, 0xFF0D1013)).size(PANEL_WIDTH, 36));

        var actionsPanel = new Flow(GuiAxis.Y)
                .background(sectionBackdrop(PANEL_WIDTH - CONTENT_LEFT * 2, 65, 0xEE20262B, 0xEE11161A))
                .padding(5)
                .margin(5)
                .size(PANEL_WIDTH - CONTENT_LEFT * 2, 65)
                .pos(CONTENT_LEFT, ACTIONS_Y);

        panel.child(actionsPanel);

        panel.child(new IDrawable.DrawableWidget(colorStripe(citadel.colour, 0, 0, 6, PANEL_HEIGHT)).size(6, PANEL_HEIGHT));
        panel.child(ModularGuiStyle.panelCloseButton(PANEL_WIDTH));

        panel.child(IKey.str(hasFaction ? citadel.getClaimDisplayName() : "Unclaimed Citadel").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y)
                .style(net.minecraft.util.text.TextFormatting.BOLD)
                .color(hasFaction ? citadel.colour : 0xFFFFFF)
                .shadow(true)
                .scale(1.2f));
        panel.child(IKey.str(hasFaction ? "Faction vault, banner relay, and command center" : "Claimed by the placer until a faction is founded").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y + 14)
                .color(0xC7CCD1));

        yeldPanel.child(new RichTextWidget()
                .textBuilder(text -> text.add(TextFormatting.BOLD + "Yield Storage").textColor(0xFFFFFF))
                .tooltip(tooltip -> tooltip.addLine("Resources collected from claimed territory"))
                .width(90)
                .height(10)
                .margin(0, 0, 0, 5)
        );


        Grid yieldGrid = new Grid()
                .mapTo(3, TileEntityCitadel.NUM_YIELD_STACKS, slot ->
                        new ItemSlot()
                                .slot(new ModularSlot(handler, slot))
                                .size(SLOT_SIZE))
                .size(3 * SLOT_SIZE, 3 * SLOT_SIZE);
        yeldPanel.child(yieldGrid);

        flagPanel.child(IKey.str("Faction Flag").asWidget()
                .color(0xFFFFFF)
                .right(130)
                .margin(0, 0, 0, 4)
                .style(net.minecraft.util.text.TextFormatting.BOLD));
        flagPanel.child(IKey.str(citadel.factionFlagId.isEmpty() ? "Choose once. This cannot be changed later." : "Locked for this faction").asWidget()
                .margin(0, 0, 0, 4 )
                .color(0xB8BDC3));
        if (!citadel.factionFlagId.isEmpty()) {
            flagPanel.child(new IDrawable.DrawableWidget(new FlagDrawable(citadel.factionFlagId)).size(56, 32).pos(228, STORAGE_Y + 32));
            flagPanel.child(IKey.str(citadel.factionFlagId).asWidget().pos(288, STORAGE_Y + 42).color(0xD6DBE0));
        } else if (hasFaction) {
            var chooseBtn = openButton("Choose", 228, () -> FactionFlagSelectGuiFactory.INSTANCE.openClient(citadel.getFaction()));
            flagPanel.child(chooseBtn.width(50));
        } else {
            panel.child(IKey.str("Create a faction first").asWidget().pos(180, STORAGE_Y + 42).color(0xD6DBE0));
        }

        actionsPanel.child(IKey.str("Command Surface").asWidget()
                .margin(0, 0, 0, 4)
                .color(0xFFFFFF)
                .style(net.minecraft.util.text.TextFormatting.BOLD)
                .right(220)
        )
        ;

        var firsRow = new Flow(GuiAxis.X)
                .padding(2)
                .margin(2)
                .height(18);
        actionsPanel.child(firsRow);
        if (hasFaction) {
            var secondRow = new Flow(GuiAxis.X)
                    .padding(2)
                    .margin(2)
                    .height(18);
            firsRow.child(openButton("Insurance", 75, () -> FactionInsuranceGuiFactory.INSTANCE.openClient(citadel.getFaction())));
            firsRow.child(openButton("Faction Stats", 75, () -> FactionStatsGuiFactory.INSTANCE.openClient(citadel.getFaction())));
            firsRow.child(openButton("Members", 75, () -> FactionMemberManagerGuiFactory.INSTANCE.openClient(FactionMemberManagerGuiData.Page.MEMBERS)));

            if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
                secondRow.child(openButton("Upgrade", 75, () -> FactionUpgradeGuiFactory.INSTANCE.openClient(citadel.getFaction())));
            }
            secondRow.child(openButton("Recolor", 75, () -> Minecraft.getMinecraft().player.openGui(
                    WarForgeMod.INSTANCE,
                    CommonProxy.GUI_TYPE_RECOLOUR_FACTION,
                    Minecraft.getMinecraft().world,
                    citadel.getPos().getX(),
                    citadel.getPos().getY(),
                    citadel.getPos().getZ()
            )));
            secondRow.child(dangerButton("Disband", 75));
            actionsPanel.child(secondRow);
        } else {
            var columnStart = new Flow(GuiAxis.Y);
            columnStart.child(openButton("Create Faction", 100, () -> Minecraft.getMinecraft().player.openGui(
                    WarForgeMod.INSTANCE,
                    CommonProxy.GUI_TYPE_CREATE_FACTION,
                    Minecraft.getMinecraft().world,
                    citadel.getPos().getX(),
                    citadel.getPos().getY(),
                    citadel.getPos().getZ()
            )).margin(2));
            columnStart.child(IKey.str("The placer can establish the faction from here.").asWidget()
                    .color(0xB8BDC3).margin(2));
            firsRow.child(columnStart);
        }

        panel.child(new ParentWidget<>().child(SlotGroupWidget.playerInventory(false)).background(GuiTextures.MC_BACKGROUND).coverChildren().margin(5).padding(5).horizontalCenter().top(PANEL_HEIGHT));


        return panel;
    }

    private static IDrawable sectionBackdrop(int width, int height, int fillColor, int borderColor) {
        return (context, drawX, drawY, drawWidth, drawHeight, theme) -> {
            Gui.drawRect(drawX, drawY, drawX + width, drawY + height, fillColor);
            Gui.drawRect(drawX, drawY, drawX + width, drawY + 1, borderColor);
            Gui.drawRect(drawX, drawY + height - 1, drawX + width, drawY + height, borderColor);
            Gui.drawRect(drawX, drawY, drawX + 1, drawY + height, borderColor);
            Gui.drawRect(drawX + width - 1, drawY, drawX + width, drawY + height, borderColor);
        };
    }

    private static IDrawable colorStripe(int color, int x, int y, int width, int height) {
        return (context, drawX, drawY, drawWidth, drawHeight, theme) -> Gui.drawRect(drawX, drawY, drawX + width, drawY + height, 0xFF000000 | (color & 0x00FFFFFF));
    }

    private static ButtonWidget<?> openButton(String label, int width, Runnable action) {
        ButtonWidget<?> button = ModularGuiStyle.actionButton(label, width, action);
        button.margin(10, 1);
        return button;
    }

    private static ButtonWidget<?> dangerButton(String label, int width) {
        ButtonWidget<?> button = ModularGuiStyle.dangerButton(label, width, () -> WarForgeMod.NETWORK.sendToServer(new PacketDisbandFaction()));
        button.margin(10, 1);
        return button;
    }
}
