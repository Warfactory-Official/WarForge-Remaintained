package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionInsuranceGuiData;
import com.flansmod.warforge.common.network.PacketFactionInsuranceAction;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.TextFormatting;

public final class GuiFactionInsurance {
    private static final int WIDTH = 356;
    private static final int HEIGHT = 248;
    private static final int CONTENT_LEFT = 12;
    private static final int HEADER_Y = 12;
    private static final int BODY_Y = 54;
    private static final int INVENTORY_Y = 182;
    private static final int STASH_COLUMNS = 8;
    private static final int STASH_CELL_SIZE = 18;
    private static final int STASH_CELL_GAP = 2;
    private static final int STASH_ROW_HEIGHT = STASH_CELL_SIZE + STASH_CELL_GAP;
    private static final int STASH_LIST_HEIGHT = 56;

    private GuiFactionInsurance() {
    }

    public static ModularPanel buildPanel(FactionInsuranceGuiData data) {
        ModularPanel panel = ModularPanel.defaultPanel("faction_insurance")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(0, 0, WIDTH, 40, 0xFF171B1F, 0xFF0D1013)).size(WIDTH, 40));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, BODY_Y, WIDTH - CONTENT_LEFT * 2, 118, 0xEE20262B, 0xEE11161A)).size(WIDTH - CONTENT_LEFT * 2, 118).pos(CONTENT_LEFT, BODY_Y));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, INVENTORY_Y, WIDTH - CONTENT_LEFT * 2, 54, 0xEE20262B, 0xEE11161A)).size(WIDTH - CONTENT_LEFT * 2, 54).pos(CONTENT_LEFT, INVENTORY_Y));
        panel.child(new IDrawable.DrawableWidget(colorStripe(data.hasFaction ? (0xFF000000 | (data.factionColor & 0x00FFFFFF)) : 0xFF4A4A4A, 0, 0, 6, HEIGHT)).size(6, HEIGHT));
        panel.child(ButtonWidget.panelCloseButton().pos(WIDTH - 18, 8));

        panel.child(IKey.str("Insurance Stash").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y)
                .style(TextFormatting.BOLD)
                .shadow(true)
                .color(0xFFFFFF)
                .scale(1.15f));
        panel.child(IKey.str(data.hasFaction ? data.factionName : "No faction selected").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y + 15)
                .color(data.hasFaction ? data.factionColor : 0xC7CCD1)
                .style(TextFormatting.BOLD));

        if (!data.hasFaction) {
            panel.child(IKey.str("No insurance stash is available.").asWidget().pos(CONTENT_LEFT + 10, BODY_Y + 12).color(0xD5D9DE));
            return panel;
        }

        panel.child(IKey.str("Protected Reserve").asWidget()
                .pos(CONTENT_LEFT + 10, BODY_Y + 8)
                .style(TextFormatting.BOLD).color(0XFFFFFF));
        panel.child(IKey.str(data.canDeposit ? "Officers and leaders may deposit. Stored items cannot be withdrawn." : "Only officers and leaders may deposit into the stash.").asWidget()
                .pos(CONTENT_LEFT + 10, BODY_Y + 20)
                .color(0xB8BDC3));
        panel.child(IKey.str(data.canVoid ? "Leader actions: use the red X to permanently void a slot." : "Only the leader can void stored items to free space.").asWidget()
                .pos(CONTENT_LEFT + 10, BODY_Y + 30)
                .color(data.canVoid ? 0xFFCC88 : 0xB8BDC3));

        ListWidget stashList = new ListWidget();
        stashList.scrollDirection(GuiAxis.Y)
                .background(GuiTextures.SLOT_ITEM)
                .width(WIDTH - 24)
                .height(STASH_LIST_HEIGHT)
                .pos(CONTENT_LEFT, BODY_Y + 46);
        for (int slot = 0, row = 0; slot < data.slotCount; slot += STASH_COLUMNS, row++) {
            stashList.addChild(createInsuranceRow(data, slot), row);
        }
        panel.child(stashList);

        panel.child(IKey.str("Deposit or shift-click items into the stash. Matching stacks can be topped up; clearing a slot requires voiding it.").asWidget()
                .pos(CONTENT_LEFT + 10, INVENTORY_Y + 8)
                .color(0xB8BDC3));
        panel.child(com.cleanroommc.modularui.widgets.SlotGroupWidget.playerInventory(false)
                .pos(CONTENT_LEFT + 8, INVENTORY_Y + 20));

        return panel;
    }

    private static ParentWidget<?> createInsuranceRow(FactionInsuranceGuiData data, int firstSlot) {
        ParentWidget<?> row = new ParentWidget<>();
        row.size(WIDTH - 40, STASH_ROW_HEIGHT);

        for (int column = 0; column < STASH_COLUMNS; column++) {
            int slot = firstSlot + column;
            if (slot >= data.slotCount) {
                break;
            }

            row.child(createInsuranceCell(data, slot).pos(4 + column * (STASH_CELL_SIZE + STASH_CELL_GAP), 0));
        }

        return row;
    }

    private static ParentWidget<?> createInsuranceCell(FactionInsuranceGuiData data, int slot) {
        ParentWidget<?> cell = new ParentWidget<>();
        cell.size(STASH_CELL_SIZE, STASH_CELL_SIZE);
        cell.child(new ItemSlot()
                .slot(new ModularSlot(data.insuranceHandler, slot).accessibility(data.canDeposit, false))
                .size(STASH_CELL_SIZE));
        if (data.canVoid) {
            cell.child(new ButtonWidget<>()
                    .width(8)
                    .height(8)
                    .overlay(IKey.str("x").color(0xFFE0E0))
                    .background((context, x, y, width, height, theme) -> Gui.drawRect(x, y, x + width, y + height, 0xFF7A2D2D))
                    .hoverBackground((context, x, y, width, height, theme) -> Gui.drawRect(x, y, x + width, y + height, 0xFF944040))
                    .onMousePressed(mouseButton -> {
                        PacketFactionInsuranceAction packet = new PacketFactionInsuranceAction();
                        packet.slot = slot;
                        WarForgeMod.NETWORK.sendToServer(packet);
                        return true;
                    })
                    .pos(10, 0));
        }
        return cell;
    }

    private static IDrawable sectionBackdrop(int x, int y, int width, int height, int fillColor, int borderColor) {
        return (context, drawX, drawY, drawWidth, drawHeight, theme) -> {
            Gui.drawRect(drawX, drawY, drawX + width, drawY + height, fillColor);
            Gui.drawRect(drawX, drawY, drawX + width, drawY + 1, borderColor);
            Gui.drawRect(drawX, drawY + height - 1, drawX + width, drawY + height, borderColor);
            Gui.drawRect(drawX, drawY, drawX + 1, drawY + height, borderColor);
            Gui.drawRect(drawX + width - 1, drawY, drawX + width, drawY + height, borderColor);
        };
    }

    private static IDrawable colorStripe(int color, int x, int y, int width, int height) {
        return (context, drawX, drawY, drawWidth, drawHeight, theme) -> Gui.drawRect(drawX, drawY, drawX + width, drawY + height, color);
    }
}
