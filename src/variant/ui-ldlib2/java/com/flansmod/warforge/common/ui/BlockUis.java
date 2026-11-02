package com.flansmod.warforge.common.ui;

import com.flansmod.warforge.compat.Nbt;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.BlockBasicClaim;
import com.flansmod.warforge.common.blocks.BlockCitadel;
import com.flansmod.warforge.common.blocks.BlockFob;
import com.flansmod.warforge.common.blocks.BlockIslandCollector;
import com.flansmod.warforge.common.blocks.BasicClaimBlockEntity;
import com.flansmod.warforge.common.blocks.CitadelBlockEntity;
import com.flansmod.warforge.common.blocks.IslandCollectorBlockEntity;
import com.flansmod.warforge.common.network.PacketFobManagerAction;
import com.flansmod.warforge.client.util.ClientElements;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.Consumer;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

public final class BlockUis {
    private static final int SLOT_SIZE = 18;

    private BlockUis() {
    }

    public static ModularUI create(Block block, BlockUIMenuType.BlockUIHolder holder) {
        CompoundTag state = isClient(holder) ? BlockUiState.received(holder.pos) : BlockUiState.capture(holder.player.level(), holder.pos);
        if (block instanceof BlockCitadel) return citadel(holder, state);
        if (block instanceof BlockBasicClaim) return basicClaim(holder, state);
        if (block instanceof BlockFob) return fob(holder, state);
        if (block instanceof BlockIslandCollector) return islandCollector(holder, state);
        throw new IllegalArgumentException("No block UI for " + block);
    }

    private static void onServer(BlockUIMenuType.BlockUIHolder holder, Consumer<ServerPlayer> action) {
        if (holder.player instanceof ServerPlayer sp) {
            action.accept(sp);
        }
    }

    private static boolean isClient(BlockUIMenuType.BlockUIHolder holder) {
        return holder.player.level().isClientSide();
    }

    private static void closeButton(UIElement panel, int width, BlockUIMenuType.BlockUIHolder holder) {
        Button close = com.flansmod.warforge.api.modularui.WarForgeUiTheme.closeButton(panel, width, isClient(holder));
        close.setOnServerClick(event -> onServer(holder, BlockUiActions::close));
    }

    private static Button serverButton(String label, int width, BlockUIMenuType.BlockUIHolder holder, Consumer<ServerPlayer> action) {
        Button button = actionButton(label, width, true);
        button.setOnServerClick(event -> onServer(holder, action));
        return button;
    }

    private static ItemSlot slot(ItemSlot slot) {
        slot.layout(l -> l.width(SLOT_SIZE).height(SLOT_SIZE).flexShrink(0));
        return vanillaSlots(slot);
    }

    private static final int CITADEL_WIDTH = 350;
    private static final int CITADEL_HEIGHT = 214;
    private static final int CITADEL_STORAGE_Y = 50;
    private static final int CITADEL_ACTIONS_Y = 140;

    private static ModularUI citadel(BlockUIMenuType.BlockUIHolder holder, CompoundTag state) {
        boolean client = isClient(holder);
        BlockEntity be = holder.player.level().getBlockEntity(holder.pos);

        UIElement root = screenRoot();
        UIElement panel = panel(CITADEL_WIDTH, CITADEL_HEIGHT, client);
        root.addChild(panel);

        if (be instanceof CitadelBlockEntity citadel) {
            boolean hasFaction = Nbt.getBoolean(state, BlockUiState.HAS_FACTION);
            int stripe = BlockUiState.stripe(state, NO_FACTION_STRIPE);

            UIElement yieldSection = place(panel, box(100, 80, sectionBackdrop()), CONTENT_LEFT, CITADEL_STORAGE_Y, 100, 80);
            place(yieldSection, tooltip(boldText(tr("warforge.ui.yield_storage"), TEXT_PRIMARY), tr("warforge.ui.resources_collected_from_claimed_territory")), 5, 5, 90, 10);
            UIElement grid = place(yieldSection, new UIElement(), (100 - 3 * SLOT_SIZE) / 2f, 20, 3 * SLOT_SIZE, 3 * SLOT_SIZE);
            grid.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(dev.vfyjxf.taffy.style.FlexWrap.WRAP));
            for (int i = 0; i < Nbt.getInt(state, BlockUiState.SLOTS); i++) {
                grid.addChild(slot(new ItemSlot().bind(citadel, i)));
            }

            int flagX = CONTENT_LEFT + 110;
            place(panel, box(215, 80, sectionBackdrop()), flagX, CITADEL_STORAGE_Y, 215, 80);
            UIElement flagSection = section(210, 80);
            flagSection.style(s -> s.background(IGuiTexture.EMPTY));
            place(panel, flagSection, flagX, CITADEL_STORAGE_Y, 210, 80);
            Label flagTitle = fromEnd(fit(text(tr("warforge.ui.faction_flag"), TEXT_PRIMARY)), 130, 5);
            flagTitle.layout(l -> l.marginBottom(5));
            flagSection.addChild(flagTitle);
            String flagId = Nbt.getString(state, BlockUiState.FLAG_ID);
            if (!flagId.isEmpty()) {
                place(flagSection, flagPreview(flagId, client), (210 - 95) / 2f, 20, 95, 54);
            } else {
                flagSection.addChild(wrap(text(tr("warforge.ui.choose_once_this_cannot_be_changed_later"), TEXT_MUTED), 210, client));
                if (hasFaction) {
                    flagSection.addChild(serverButton(tr("warforge.ui.choose"), 50, holder, sp -> BlockUiActions.chooseFlag(sp, citadel.getFaction())));
                } else {
                    place(panel, fit(text(tr("warforge.ui.create_a_faction_first"), TEXT_HINT)), 180, CITADEL_STORAGE_Y + 42);
                }
            }

            headerBackdrop(panel, CITADEL_WIDTH);

            int actionsWidth = CITADEL_WIDTH - CONTENT_LEFT * 2;
            UIElement actions = section(panel, CONTENT_LEFT, CITADEL_ACTIONS_Y, actionsWidth, 65);
            Label actionsTitle = fromEnd(fit(text(tr("warforge.ui.command_surface"), TEXT_PRIMARY)), 220, 5);
            actionsTitle.layout(l -> l.marginBottom(4));
            actions.addChild(actionsTitle);
            if (hasFaction) {
                int rowX = 5 + 2 + 10;
                int[] columns = {rowX, rowX + 95, rowX + 190};
                place(actions, serverButton(tr("warforge.ui.insurance"), 75, holder, sp -> BlockUiActions.openInsurance(sp, citadel.getFaction())),
                        columns[0], 19, 75, BUTTON_HEIGHT);
                place(actions, serverButton(tr("warforge.ui.faction_stats"), 75, holder, sp -> BlockUiActions.showFactionInfo(sp, citadel.getFaction())),
                        columns[1], 19, 75, BUTTON_HEIGHT);
                place(actions, serverButton(tr("warforge.ui.members"), 75, holder, BlockUiActions::openMembers),
                        columns[2], 19, 75, BUTTON_HEIGHT);
                int column = 0;
                if (Nbt.getBoolean(state, BlockUiState.UPGRADES)) {
                    place(actions, serverButton(tr("warforge.ui.upgrade"), 75, holder, sp -> BlockUiActions.openUpgrade(sp, citadel.getFaction())),
                            columns[column++], 41, 75, BUTTON_HEIGHT);
                }
                place(actions, serverButton(tr("warforge.ui.recolor"), 75, holder, sp -> BlockUiActions.openRecolour(sp, citadel)),
                        columns[column++], 41, 75, BUTTON_HEIGHT);
                Button disband = dangerButton(tr("warforge.ui.disband"), 75, null);
                disband.setOnServerClick(event -> onServer(holder, BlockUiActions::disband));
                place(actions, disband, columns[column], 41, 75, BUTTON_HEIGHT);
            } else {
                UIElement column = flow(FlexDirection.COLUMN);
                column.layout(l -> l.width(actionsWidth - 10).marginTop(2));
                Button create = serverButton(tr("warforge.ui.create_faction"), 100, holder, sp -> BlockUiActions.openCreateFaction(sp, holder.pos));
                create.layout(l -> l.marginAll(2));
                column.addChild(create);
                Label placer = fit(text(tr("warforge.ui.the_placer_can_establish_the_faction_from_here"), TEXT_MUTED));
                placer.layout(l -> l.marginAll(2));
                column.addChild(placer);
                actions.addChild(column);
            }

            colorStripe(panel, stripe, CITADEL_HEIGHT);
            closeButton(panel, CITADEL_WIDTH, holder);

            String title = hasFaction ? Nbt.getString(state, BlockUiState.TITLE) : tr("warforge.ui.unclaimed_citadel");
            String subtitle = hasFaction ? tr("warforge.ui.faction_vault_banner_relay_and_command_center")
                    : tr("warforge.ui.claimed_by_the_placer_until_a_faction_is_founded");
            title(panel, title, hasFaction ? stripe : TEXT_PRIMARY);
            subtitle(panel, text(subtitle, TEXT_SECONDARY));
        } else {
            headerBackdrop(panel, CITADEL_WIDTH);
            closeButton(panel, CITADEL_WIDTH, holder);
        }

        root.addChild(inventoryPanel(client, INVENTORY_GAP));
        return ModularUI.of(UI.of(root), holder.player);
    }

    private static UIElement flagPreview(String flagId, boolean client) {
        return client ? ClientElements.flag(flagId) : new UIElement();
    }

    private static final int CLAIM_WIDTH = 200;
    private static final int CLAIM_HEIGHT = 182;
    private static final int CLAIM_GRID_Y = 30;

    private static ModularUI basicClaim(BlockUIMenuType.BlockUIHolder holder, CompoundTag state) {
        boolean client = isClient(holder);
        UIElement root = screenRoot();
        UIElement panel = panel(CLAIM_WIDTH, CLAIM_HEIGHT, client);
        root.addChild(panel);

        if (holder.player.level().getBlockEntity(holder.pos) instanceof BasicClaimBlockEntity claim) {
            place(panel, boldText(Nbt.getString(state, BlockUiState.TITLE), TEXT_PRIMARY), CONTENT_LEFT, 8, CLAIM_WIDTH - CONTENT_LEFT * 2 - 18, 9);
            place(panel, text(tr("warforge.ui.yields"), TEXT_SECONDARY), CONTENT_LEFT, 20, 60, 9);

            int slots = Nbt.getInt(state, BlockUiState.SLOTS);
            int rows = (slots + 2) / 3;
            UIElement grid = place(panel, new UIElement(), CONTENT_LEFT, CLAIM_GRID_Y, 3 * SLOT_SIZE, rows * SLOT_SIZE);
            grid.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(dev.vfyjxf.taffy.style.FlexWrap.WRAP));
            for (int i = 0; i < slots; i++) {
                grid.addChild(slot(new ItemSlot().bind(claim, i)));
            }

            int actionsX = CONTENT_LEFT + 3 * SLOT_SIZE + 12;
            Button info = vanillaButton(tr("warforge.ui.info"), 100, 20);
            info.setOnServerClick(event -> onServer(holder, sp -> BlockUiActions.showFactionInfo(sp, claim.getFaction())));
            place(panel, info, actionsX, CLAIM_GRID_Y + 2, 100, 20);
            Button unclaim = vanillaButton(tr("warforge.ui.unclaim"), 100, 20);
            unclaim.setOnServerClick(event -> onServer(holder, sp -> BlockUiActions.unclaim(sp, claim.getClaimPos())));
            place(panel, unclaim, actionsX, CLAIM_GRID_Y + 26, 100, 20);
            Button moveCitadel = vanillaButton(tr("warforge.ui.move_citadel"), 100, 20);
            moveCitadel.setOnServerClick(event -> onServer(holder, BlockUiActions::moveCitadel));
            place(panel, moveCitadel, actionsX, CLAIM_GRID_Y + 50, 100, 20);
        }

        com.flansmod.warforge.api.modularui.WarForgeUiTheme.closeButton(panel, CLAIM_WIDTH)
                .setOnServerClick(event -> onServer(holder, BlockUiActions::close));
        place(panel, vanillaSlots(new InventorySlots()), (CLAIM_WIDTH - 162) / 2f, CLAIM_HEIGHT - 7 - 77);
        return ModularUI.of(UI.of(root), holder.player);
    }

    private static final int FOB_WIDTH = 300;
    private static final int FOB_HEIGHT = 150;

    private static ModularUI fob(BlockUIMenuType.BlockUIHolder holder, CompoundTag state) {
        boolean client = isClient(holder);
        int sectionWidth = FOB_WIDTH - CONTENT_LEFT * 2;

        UIElement root = screenRoot();
        UIElement panel = panel(FOB_WIDTH, FOB_HEIGHT, client);
        root.addChild(panel);

        headerBackdrop(panel, FOB_WIDTH);
        place(panel, box(sectionWidth, FOB_HEIGHT - BODY_Y - 12, sectionBackdrop()),
                CONTENT_LEFT, BODY_Y, sectionWidth, FOB_HEIGHT - BODY_Y - 12);
        colorStripe(panel, BlockUiState.stripe(state, NO_FACTION_STRIPE), FOB_HEIGHT);
        closeButton(panel, FOB_WIDTH, holder);
        title(panel, tr("warforge.ui.forward_operating_base"), TEXT_PRIMARY);

        if (Nbt.getBoolean(state, BlockUiState.PRESENT)) {
            float buttonX = (FOB_WIDTH - 90) / 2f;
            if (Nbt.getBoolean(state, BlockUiState.ESTABLISHED)) {
                int tickets = Nbt.getInt(state, BlockUiState.TICKETS);
                boolean canWarp = Nbt.getBoolean(state, BlockUiState.CAN_WARP);
                subtitle(panel, text(Nbt.getString(state, BlockUiState.NAME), TEXT_PRIMARY));
                Label ticketLabel = text(UiLang.tr("warforge.ui.warp_tickets_d_d", tickets, Nbt.getInt(state, BlockUiState.MAX_TICKETS)),
                        canWarp ? TEXT_SUCCESS : TEXT_WARNING);
                ticketLabel.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER));
                place(panel, ticketLabel, CONTENT_LEFT, BODY_Y + 8, sectionWidth, 9);
                Button warp = actionButton(tr("warforge.ui.warp"), 90, canWarp);
                if (canWarp) {
                    warp.setOnServerClick(event -> onServer(holder, sp -> BlockUiActions.warpToFob(sp, holder.pos)));
                }
                place(panel, warp, buttonX, BODY_Y + 34, 90, BUTTON_HEIGHT);
            } else {
                subtitle(panel, text(tr("warforge.ui.enter_a_name_to_establish_this_fob"), TEXT_SECONDARY));
                TextField name = new TextField().setText("").setAnyString();
                name.style(s -> s.background(insetBackdrop()));
                place(panel, name, CONTENT_LEFT, BODY_Y + 8, sectionWidth - 16, 18);
                Button establish = actionButton(tr("warforge.ui.establish"), 90, true);
                establish.setOnClick(event -> {
                    String value = name.getValue().trim();
                    if (value.isEmpty()) {
                        return;
                    }
                    PacketFobManagerAction packet = new PacketFobManagerAction();
                    packet.action = PacketFobManagerAction.Action.ESTABLISH;
                    packet.target = new DimBlockPos(holder.player.level().dimension(), holder.pos);
                    packet.name = value;
                    WarForgeMod.NETWORK.sendToServer(packet);
                    holder.player.closeContainer();
                });
                place(panel, establish, buttonX, BODY_Y + 34, 90, BUTTON_HEIGHT);
            }
        }

        return ModularUI.of(UI.of(root), holder.player);
    }

    private static final int COLLECTOR_COLUMNS = 10;
    private static final int COLLECTOR_VISIBLE_ROWS = 6;
    private static final int COLLECTOR_LIST_WIDTH = COLLECTOR_COLUMNS * SLOT_SIZE;
    private static final int COLLECTOR_LIST_HEIGHT = COLLECTOR_VISIBLE_ROWS * SLOT_SIZE;
    private static final int COLLECTOR_SECTION_WIDTH = COLLECTOR_LIST_WIDTH + 10;
    private static final int COLLECTOR_SECTION_HEIGHT = COLLECTOR_LIST_HEIGHT + 28;
    private static final int COLLECTOR_WIDTH = CONTENT_LEFT * 2 + COLLECTOR_SECTION_WIDTH;
    private static final int COLLECTOR_HEIGHT = BODY_Y + COLLECTOR_SECTION_HEIGHT + 12;

    private static ModularUI islandCollector(BlockUIMenuType.BlockUIHolder holder, CompoundTag state) {
        boolean client = isClient(holder);

        UIElement root = screenRoot();
        root.addChild(box(1, INVENTORY_BOX_HEIGHT));
        UIElement panel = panel(COLLECTOR_WIDTH, COLLECTOR_HEIGHT, client);
        root.addChild(panel);

        BlockEntity be = holder.player.level().getBlockEntity(holder.pos);
        IslandCollectorBlockEntity collector = be instanceof IslandCollectorBlockEntity c ? c : null;
        boolean hasFaction = Nbt.getBoolean(state, BlockUiState.HAS_FACTION);

        place(panel, box(COLLECTOR_WIDTH, HEADER_HEIGHT, fill(HEADER_FILL)), 0, 0, COLLECTOR_WIDTH, HEADER_HEIGHT);
        UIElement storage = place(panel, section(COLLECTOR_SECTION_WIDTH, COLLECTOR_SECTION_HEIGHT),
                CONTENT_LEFT, BODY_Y, COLLECTOR_SECTION_WIDTH, COLLECTOR_SECTION_HEIGHT);
        storage.style(s -> s.background(fill(SECTION_FILL)));
        colorStripe(panel, hasFaction ? Nbt.getInt(state, BlockUiState.COLOUR) : NO_FACTION_STRIPE, COLLECTOR_HEIGHT);
        closeButton(panel, COLLECTOR_WIDTH, holder);

        title(panel, tr("warforge.ui.faction_yield_storage"), TEXT_PRIMARY);
        subtitle(panel, text(hasFaction ? Nbt.getString(state, BlockUiState.FACTION_LABEL) : tr("warforge.ui.unclaimed"), hasFaction ? Nbt.getInt(state, BlockUiState.COLOUR) : TEXT_SECONDARY));

        Label heading = fit(text(tr("warforge.ui.collected_yield"), TEXT_PRIMARY));
        heading.layout(l -> l.marginBottom(4));
        storage.addChild(heading);

        if (collector != null) {
            IItemHandlerModifiable handler = collector.getStorageHandler();
            int slots = Nbt.getInt(state, BlockUiState.SLOTS);
            int rows = (slots + COLLECTOR_COLUMNS - 1) / COLLECTOR_COLUMNS;
            IGuiTexture inset = fill(INSET_FILL);
            ScrollerView list = insetList(COLLECTOR_LIST_WIDTH + (rows > COLLECTOR_VISIBLE_ROWS ? 5 : 0), COLLECTOR_LIST_HEIGHT, inset);
            for (int row = 0; row < rows; row++) {
                UIElement rowElement = row(COLLECTOR_LIST_WIDTH, SLOT_SIZE);
                for (int col = 0; col < COLLECTOR_COLUMNS; col++) {
                    int index = row * COLLECTOR_COLUMNS + col;
                    if (index >= slots) {
                        break;
                    }
                    rowElement.addChild(slot(new ItemSlot().bind(new SlotItemHandler(handler, index, 0, 0) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }
                    })));
                }
                list.addScrollViewChild(rowElement);
            }
            storage.addChild(list);
        }

        root.addChild(inventoryPanel(client, 0));
        return ModularUI.of(UI.of(root), holder.player);
    }
}
