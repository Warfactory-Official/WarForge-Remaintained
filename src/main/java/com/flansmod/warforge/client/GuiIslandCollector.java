package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.flansmod.warforge.common.blocks.TileEntityIslandCollector;
import com.flansmod.warforge.server.Faction;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * ModularUI panel for the faction yield collector.
 * <p>
 * Kept free of any {@code net.minecraft.client.*} references at build time so it can be constructed
 * on both the client and a dedicated server (ModularUI builds the panel on both sides to wire up
 * slot syncing). The 100 storage slots are extract-only: players may pull items out but never put
 * items in.
 */
public final class GuiIslandCollector {
    private static final int COLUMNS = 10;
    private static final int SLOT_SIZE = 18;
    private static final int VISIBLE_ROWS = 6;
    private static final int CONTENT_LEFT = 8;
    private static final int LIST_TOP = 34;
    private static final int LIST_WIDTH = COLUMNS * SLOT_SIZE;
    private static final int WIDTH = CONTENT_LEFT * 2 + LIST_WIDTH + 6;
    private static final int HEIGHT = LIST_TOP + VISIBLE_ROWS * SLOT_SIZE + 12;

    private GuiIslandCollector() {
    }

    public static ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings, TileEntityIslandCollector collector) {
        syncManager.bindPlayerInventory(data.getPlayer());

        IItemHandlerModifiable storage = collector.getStorageHandler();
        int slots = storage.getSlots();
        boolean hasFaction = !collector.getFaction().equals(Faction.nullUuid);

        ModularPanel panel = ModularPanel.defaultPanel("island_collector")
                .width(WIDTH)
                .height(HEIGHT);

        panel.child(IKey.str("Faction Yield Storage").asWidget()
                .pos(CONTENT_LEFT, 8)
                .style(TextFormatting.BOLD)
                .color(0xFFFFFF)
                .shadow(true));
        panel.child(IKey.str(hasFaction ? collector.factionName : "Unclaimed").asWidget()
                .pos(CONTENT_LEFT, 20)
                .color(hasFaction ? collector.colour : 0xB8BDC3));

        ListWidget list = new ListWidget();
        list.scrollDirection(GuiAxis.Y)
                .name("island_collector_list")
                .width(LIST_WIDTH)
                .height(VISIBLE_ROWS * SLOT_SIZE)
                .pos(CONTENT_LEFT, LIST_TOP);

        int rows = (slots + COLUMNS - 1) / COLUMNS;
        for (int row = 0; row < rows; row++) {
            list.addChild(buildRow(storage, row, slots), row);
        }
        panel.child(list);

        panel.child(new ParentWidget<>()
                .child(SlotGroupWidget.playerInventory(false))
                .background(GuiTextures.MC_BACKGROUND)
                .coverChildren()
                .padding(4)
                .pos(CONTENT_LEFT, LIST_TOP + VISIBLE_ROWS * SLOT_SIZE + 8));

        return panel;
    }

    private static ParentWidget<?> buildRow(IItemHandlerModifiable storage, int row, int slots) {
        ParentWidget<?> rowWidget = new ParentWidget<>();
        rowWidget.name("island_collector_row_" + row);
        rowWidget.size(LIST_WIDTH, SLOT_SIZE);

        for (int col = 0; col < COLUMNS; col++) {
            int slot = row * COLUMNS + col;
            if (slot >= slots) {
                break;
            }
            rowWidget.child(new ItemSlot()
                    .name("island_collector_slot_" + slot)
                    // canPut = false (nothing can be put in), canTake = true (players may extract)
                    .slot(new ModularSlot(storage, slot).accessibility(false, true))
                    .size(SLOT_SIZE)
                    .pos(col * SLOT_SIZE, 0));
        }
        return rowWidget;
    }
}
