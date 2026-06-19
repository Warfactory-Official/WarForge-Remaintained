package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
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
    private static final int CONTENT_LEFT = 12;
    private static final int HEADER_Y = 12;
    private static final int HEADER_HEIGHT = 40;
    private static final int BODY_Y = 54;
    private static final int LIST_WIDTH = COLUMNS * SLOT_SIZE;
    private static final int LIST_HEIGHT = VISIBLE_ROWS * SLOT_SIZE;
    private static final int SECTION_WIDTH = LIST_WIDTH + 10;
    private static final int SECTION_HEIGHT = LIST_HEIGHT + 28;
    private static final int WIDTH = CONTENT_LEFT * 2 + SECTION_WIDTH;
    private static final int HEIGHT = BODY_Y + SECTION_HEIGHT + 12;

    private GuiIslandCollector() {
    }

    public static ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings, TileEntityIslandCollector collector) {
        syncManager.bindPlayerInventory(data.getPlayer());

        IItemHandlerModifiable storage = collector.getStorageHandler();
        int slots = storage.getSlots();
        boolean hasFaction = !collector.getFaction().equals(Faction.nullUuid);

        ModularPanel panel = ModularPanel.defaultPanel("island_collector")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        Flow storageSection = ModularGuiStyle.section(SECTION_WIDTH, SECTION_HEIGHT)
                .name("island_collector_storage_section")
                .pos(CONTENT_LEFT, BODY_Y);

        panel.child(new IDrawable.DrawableWidget(ModularGuiStyle.headerBackdrop())
                .name("island_collector_header_backdrop")
                .size(WIDTH, HEADER_HEIGHT));
        panel.child(storageSection);
        panel.child(new IDrawable.DrawableWidget(ModularGuiStyle.colorStripe(hasFaction ? collector.colour : 0x4A4A4A))
                .name("island_collector_color_stripe")
                .size(6, HEIGHT));
        panel.child(ModularGuiStyle.panelCloseButton(WIDTH));

        panel.child(IKey.str("Faction Yield Storage").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y)
                .style(TextFormatting.BOLD)
                .color(ModularGuiStyle.TEXT_PRIMARY)
                .shadow(true)
                .scale(1.15f));
        panel.child(IKey.str(hasFaction ? collector.factionName : "Unclaimed").asWidget()
                .pos(CONTENT_LEFT, HEADER_Y + 15)
                .style(TextFormatting.BOLD)
                .color(hasFaction ? collector.colour : ModularGuiStyle.TEXT_SECONDARY));

        storageSection.child(IKey.str("Collected Yield").asWidget()
                .margin(0, 0, 0, 4)
                .style(TextFormatting.BOLD)
                .color(ModularGuiStyle.TEXT_PRIMARY));

        ListWidget list = new ListWidget();
        list.scrollDirection(GuiAxis.Y)
                .name("island_collector_list")
                .background(ModularGuiStyle.insetBackdrop())
                .width(LIST_WIDTH)
                .height(LIST_HEIGHT);

        int rows = (slots + COLUMNS - 1) / COLUMNS;
        for (int row = 0; row < rows; row++) {
            list.addChild(buildRow(storage, row, slots), row);
        }
        storageSection.child(list);

        panel.child(ModularGuiStyle.playerInventoryPanel(HEIGHT));

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
