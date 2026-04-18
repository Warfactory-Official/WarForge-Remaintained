package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionInsuranceGuiData;
import com.flansmod.warforge.common.network.PacketFactionInsuranceAction;
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
        int sectionWidth = WIDTH - CONTENT_LEFT * 2;

        ModularPanel panel = ModularPanel.defaultPanel("faction_insurance")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        Flow bodySection = ModularGuiStyle.section(sectionWidth, 118).name("insurance_body_section").pos(CONTENT_LEFT, BODY_Y);
        Flow inventorySection = ModularGuiStyle.section(sectionWidth, 54).name("insurance_inventory_section").pos(CONTENT_LEFT, INVENTORY_Y);

        panel.child(new com.cleanroommc.modularui.api.drawable.IDrawable.DrawableWidget(ModularGuiStyle.headerBackdrop()).size(WIDTH, 40));
        panel.child(bodySection);
        panel.child(inventorySection);
        panel.child(new com.cleanroommc.modularui.api.drawable.IDrawable.DrawableWidget(ModularGuiStyle.colorStripe(data.hasFaction ? data.factionColor : 0x4A4A4A)).size(6, HEIGHT));
        panel.child(ModularGuiStyle.panelCloseButton(WIDTH));

        panel.child(IKey.str("Insurance Stash").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y)
                .style(TextFormatting.BOLD)
                .shadow(true)
                .color(ModularGuiStyle.TEXT_PRIMARY)
                .scale(1.15f));
        panel.child(IKey.str(data.hasFaction ? data.factionName : "No faction selected").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y + 15)
                .color(data.hasFaction ? data.factionColor : ModularGuiStyle.TEXT_SECONDARY)
                .style(TextFormatting.BOLD));

        if (!data.hasFaction) {
            bodySection.child(IKey.str("No insurance stash is available.").asWidget()
                    .margin(0, 6)
                    .color(0xD5D9DE));
            return panel;
        }

        bodySection.child(IKey.str("Protected Reserve").asWidget()
                .margin(0, 0, 0, 4)
                .style(TextFormatting.BOLD)
                .color(ModularGuiStyle.TEXT_PRIMARY));
        bodySection.child(IKey.str(data.canDeposit ? "Officers and leaders may deposit. Stored items cannot be withdrawn." : "Only officers and leaders may deposit into the stash.").asWidget()
                .margin(0, 0, 0, 2)
                .color(ModularGuiStyle.TEXT_MUTED));
        bodySection.child(IKey.str(data.canVoid ? "Leader actions: use the red X to permanently void a slot." : "Only the leader can void stored items to free space.").asWidget()
                .margin(0, 0, 0, 4)
                .color(data.canVoid ? 0xFFCC88 : ModularGuiStyle.TEXT_MUTED));

        ListWidget stashList = new ListWidget();
        stashList.scrollDirection(GuiAxis.Y)
                .name("insurance_stash_list")
                .background(ModularGuiStyle.insetBackdrop())
                .width(sectionWidth - 10)
                .height(STASH_LIST_HEIGHT);
        for (int slot = 0, row = 0; slot < data.slotCount; slot += STASH_COLUMNS, row++) {
            stashList.addChild(createInsuranceRow(data, slot), row);
        }
        bodySection.child(stashList);

        inventorySection.child(IKey.str("Deposit or shift-click items into the stash. Matching stacks can be topped up; clearing a slot requires voiding it.").asWidget()
                .margin(0, 0, 0, 4)
                .color(ModularGuiStyle.TEXT_MUTED));
        inventorySection.child(new ParentWidget<>()
                .child(SlotGroupWidget.playerInventory(false))
                .background(GuiTextures.MC_BACKGROUND)
                .coverChildren()
                .padding(4));

        return panel;
    }

    private static ParentWidget<?> createInsuranceRow(FactionInsuranceGuiData data, int firstSlot) {
        ParentWidget<?> row = new ParentWidget<>();
        row.name("insurance_row_" + (firstSlot / STASH_COLUMNS));
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
        cell.name("insurance_cell_" + slot);
        cell.size(STASH_CELL_SIZE, STASH_CELL_SIZE);
        cell.child(new ItemSlot()
                .name("insurance_slot_" + slot)
                .slot(new ModularSlot(data.insuranceHandler, slot).accessibility(data.canDeposit, false))
                .size(STASH_CELL_SIZE));
        if (data.canVoid) {
            ButtonWidget<?> voidButton = ModularGuiStyle.smallDangerButton("x", 8, 8, () -> {
                PacketFactionInsuranceAction packet = new PacketFactionInsuranceAction();
                packet.slot = slot;
                WarForgeMod.NETWORK.sendToServer(packet);
            });
            voidButton.name("insurance_void_slot_" + slot);
            cell.child(voidButton.pos(10, 0));
        }
        return cell;
    }
}
