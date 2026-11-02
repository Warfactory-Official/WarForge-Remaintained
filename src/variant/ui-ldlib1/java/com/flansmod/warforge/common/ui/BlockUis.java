package com.flansmod.warforge.common.ui;

import com.flansmod.warforge.compat.Nbt;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.ScrollArea;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.BasicClaimBlockEntity;
import com.flansmod.warforge.common.blocks.CitadelBlockEntity;
import com.flansmod.warforge.common.blocks.IslandCollectorBlockEntity;
import com.flansmod.warforge.client.util.ClientElements;
import com.flansmod.warforge.common.network.PacketFobManagerAction;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.UUID;
import java.util.function.Consumer;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

/** LDLib1 routes presses by widget tree position => tree (widgets, order) depends only on the state tag; sizes may differ. */
public final class BlockUis {
    private static final int SLOT_SIZE = 18;
    private static final int FOB_NAME_MAX = 32;

    private BlockUis() {
    }

    public static ModularUI create(BlockUiHolder holder, Player player) {
        WidgetGroup root = switch (Nbt.getString(holder.state, BlockUiState.KIND)) {
            case BlockUiState.CITADEL -> citadel(holder, player);
            case BlockUiState.CLAIM -> basicClaim(holder, player);
            case BlockUiState.FOB -> fob(holder, player);
            case BlockUiState.COLLECTOR -> islandCollector(holder, player);
            default -> throw new IllegalArgumentException("No block UI for " + holder.block);
        };
        return new ModularUI(root, holder, player);
    }

    private static Consumer<ClickData> onServer(Player player, Consumer<ServerPlayer> action) {
        return click -> {
            if (!click.isRemote && player instanceof ServerPlayer sp) {
                action.accept(sp);
            }
        };
    }

    private static ButtonWidget closeButton(int panelWidth, Player player) {
        return WarForgeUiTheme.closeButton(panelWidth, onServer(player, BlockUiActions::close));
    }

    private static IItemTransfer transfer(BlockEntity be, int slots) {
        if (be instanceof IItemHandlerModifiable handler) {
            return ItemTransferHelperImpl.toItemTransfer(handler);
        }
        return new ItemStackTransfer(slots);
    }

    private static SlotWidget slot(IItemTransfer transfer, int index, boolean canPut) {
        return canPut
                ? new SlotWidget(transfer, index, 0, 0, true, true)
                : new SlotWidget(transfer, index, 0, 0, true, false) {
                    @Override
                    public boolean canMergeSlot(ItemStack stack) {
                        return false;
                    }
                };
    }

    private static void slotGrid(WidgetGroup parent, IItemTransfer transfer, int slots, int columns, int x, int y) {
        for (int i = 0; i < slots; i++) {
            SlotWidget slot = slot(transfer, i, true);
            slot.setSelfPosition(x + (i % columns) * SLOT_SIZE, y + (i / columns) * SLOT_SIZE);
            parent.addWidget(slot);
        }
    }

    private static ButtonWidget placed(ButtonWidget button, int x, int y) {
        button.setSelfPosition(x, y);
        return button;
    }

    private static WidgetGroup citadel(BlockUiHolder holder, Player player) {
        final int width = 350;
        final int height = 214;
        final int storageY = 50;
        final int actionsY = 140;
        CompoundTag state = holder.state;

        int inventoryY = height + WarForgeUiTheme.INVENTORY_GAP;
        WidgetGroup root = WarForgeUiTheme.group(0, 0, width, inventoryY + WarForgeUiTheme.INVENTORY_HEIGHT);
        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, width, height);
        root.addWidget(panel);

        int stripe = BlockUiState.stripe(state, WarForgeUiTheme.STRIPE_DEFAULT);

        if (Nbt.getBoolean(state, BlockUiState.PRESENT)) {
            BlockEntity be = holder.blockEntity();
            CitadelBlockEntity citadel = be instanceof CitadelBlockEntity c ? c : null;
            boolean hasFaction = Nbt.getBoolean(state, BlockUiState.HAS_FACTION);

            int yieldX = CONTENT_LEFT;
            panel.addWidget(WarForgeUiTheme.section(yieldX, storageY, 100, 80));
            int flagX = CONTENT_LEFT + 110;
            int flagWidth = 210;
            panel.addWidget(WarForgeUiTheme.section(flagX, storageY, 215, 80));
            panel.addWidget(WarForgeUiTheme.header(width));
            int actionsWidth = width - CONTENT_LEFT * 2;
            panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, actionsY, actionsWidth, 65));
            panel.addWidget(WarForgeUiTheme.stripe(stripe, height));
            panel.addWidget(closeButton(width, player));

            panel.addWidget(WarForgeUiTheme.title(Nbt.getString(state, BlockUiState.TITLE), hasFaction ? stripe : WarForgeUiTheme.TEXT_PRIMARY));
            panel.addWidget(WarForgeUiTheme.subtitle(hasFaction
                    ? tr("warforge.ui.faction_vault_banner_relay_and_command_center")
                    : tr("warforge.ui.claimed_by_the_placer_until_a_faction_is_founded"), WarForgeUiTheme.TEXT_SECONDARY, false));

            panel.addWidget(Label.bold(tr("warforge.ui.yield_storage"), WarForgeUiTheme.TEXT_PRIMARY).box(90, 10)
                    .tooltip(tr("warforge.ui.resources_collected_from_claimed_territory")).at(yieldX + 5, storageY + 5));
            int slots = Nbt.getInt(state, BlockUiState.SLOTS);
            slotGrid(panel, transfer(be, slots), slots, 3, yieldX + (100 - 3 * SLOT_SIZE) / 2, storageY + 20);

            panel.addWidget(Label.bold(tr("warforge.ui.faction_flag"), WarForgeUiTheme.TEXT_PRIMARY).box(flagWidth - 130 - 5, 9).right()
                    .at(flagX + 5, storageY + 5));
            String flagId = Nbt.getString(state, BlockUiState.FLAG_ID);
            int flagTextY = storageY + 5 + 9 + 5;
            if (flagId.isEmpty()) {
                panel.addWidget(Label.text(tr("warforge.ui.choose_once_this_cannot_be_changed_later"), WarForgeUiTheme.TEXT_MUTED)
                        .wrap(flagWidth - 10).at(flagX + 5, flagTextY));
            }
            if (!flagId.isEmpty()) {
                Widget flag = holder.level.isClientSide() ? ClientElements.flag(flagId, 95, 54) : new Widget(0, 0, 95, 54);
                flag.setSelfPosition(flagX + (flagWidth - 95) / 2, storageY + 20);
                panel.addWidget(flag);
            } else if (hasFaction) {
                panel.addWidget(placed(WarForgeUiTheme.actionButton(tr("warforge.ui.choose"), 50, onServer(player, sp -> {
                    Faction faction = citadel == null ? null : WarForgeMod.FACTIONS.registry.getFaction(citadel.getFaction());
                    if (faction != null) {
                        BlockUiActions.chooseFlag(sp, faction.getUuid());
                    }
                })), flagX + 5 + 10, flagTextY + 20 + 4));
            } else {
                panel.addWidget(Label.text(tr("warforge.ui.create_a_faction_first"), WarForgeUiTheme.TEXT_HINT).at(180, storageY + 42));
            }

            int textX = CONTENT_LEFT + 5;
            panel.addWidget(Label.bold(tr("warforge.ui.command_surface"), WarForgeUiTheme.TEXT_PRIMARY).box(actionsWidth - 220 - 5, 9).right()
                    .at(textX, actionsY + 5));
            int rowX = textX + 2;
            int firstRowY = actionsY + 5 + 9 + 4 + 2;
            if (hasFaction) {
                UUID factionId = citadel == null ? FactionIds.NONE : citadel.getFaction();
                int secondRowY = firstRowY + 18 + 2 + 2;
                int[] columns = {rowX + 10, rowX + 10 + 75 + 20, rowX + 10 + (75 + 20) * 2};

                panel.addWidget(placed(WarForgeUiTheme.actionButton(tr("warforge.ui.insurance"), 75,
                        onServer(player, sp -> BlockUiActions.openInsurance(sp, factionId))), columns[0], firstRowY));
                panel.addWidget(placed(WarForgeUiTheme.actionButton(tr("warforge.ui.faction_stats"), 75,
                        onServer(player, sp -> BlockUiActions.showFactionInfo(sp, citadel.getFaction()))), columns[1], firstRowY));
                panel.addWidget(placed(WarForgeUiTheme.actionButton(tr("warforge.ui.members"), 75,
                        onServer(player, BlockUiActions::openMembers)), columns[2], firstRowY));

                int column = 0;
                if (Nbt.getBoolean(state, BlockUiState.UPGRADES)) {
                    panel.addWidget(placed(WarForgeUiTheme.actionButton(tr("warforge.ui.upgrade"), 75,
                            onServer(player, sp -> BlockUiActions.openUpgrade(sp, factionId))), columns[column++], secondRowY));
                }
                panel.addWidget(placed(WarForgeUiTheme.actionButton(tr("warforge.ui.recolor"), 75,
                        onServer(player, sp -> BlockUiActions.openRecolour(sp, citadel))), columns[column++], secondRowY));
                panel.addWidget(placed(WarForgeUiTheme.dangerButton(tr("warforge.ui.disband"), 75,
                        onServer(player, BlockUiActions::disband)), columns[column], secondRowY));
            } else {
                panel.addWidget(placed(WarForgeUiTheme.actionButton(tr("warforge.ui.create_faction"), 100,
                        onServer(player, sp -> BlockUiActions.openCreateFaction(sp, holder.pos))), rowX + 2, firstRowY + 2));
                panel.addWidget(Label.text(tr("warforge.ui.the_placer_can_establish_the_faction_from_here"), WarForgeUiTheme.TEXT_MUTED)
                        .at(rowX + 2, firstRowY + 2 + 18 + 2 + 2));
            }
        } else {
            panel.addWidget(WarForgeUiTheme.header(width));
            panel.addWidget(WarForgeUiTheme.stripe(stripe, height));
            panel.addWidget(closeButton(width, player));
            panel.addWidget(WarForgeUiTheme.title(tr("warforge.ui.citadel")));
        }

        root.addWidget(WarForgeUiTheme.inventoryBox((width - WarForgeUiTheme.INVENTORY_WIDTH) / 2, inventoryY));
        return root;
    }

    private static WidgetGroup basicClaim(BlockUiHolder holder, Player player) {
        final int width = 200;
        final int height = 182;
        final int gridY = 30;
        CompoundTag state = holder.state;

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, width, height);
        panel.addWidget(closeButton(width, player));

        if (Nbt.getBoolean(state, BlockUiState.PRESENT)) {
            BlockEntity be = holder.blockEntity();
            BasicClaimBlockEntity claim = be instanceof BasicClaimBlockEntity c ? c : null;
            panel.addWidget(Label.bold(Nbt.getString(state, BlockUiState.TITLE), WarForgeUiTheme.TEXT_PRIMARY).clip(width - CONTENT_LEFT - 22).at(CONTENT_LEFT, 8));
            panel.addWidget(Label.text(tr("warforge.ui.yields"), WarForgeUiTheme.TEXT_SECONDARY).at(CONTENT_LEFT, 20));

            int slots = Nbt.getInt(state, BlockUiState.SLOTS);
            slotGrid(panel, transfer(be, slots), slots, 3, CONTENT_LEFT, gridY);

            int actionsX = CONTENT_LEFT + 3 * SLOT_SIZE + 12;
            int y = gridY + 2;
            panel.addWidget(placed(WarForgeUiTheme.vanillaButton(tr("warforge.ui.info"), 100, 20,
                    onServer(player, sp -> BlockUiActions.showFactionInfo(sp, claim.getFaction()))), actionsX, y));
            y += 20 + 4;
            panel.addWidget(placed(WarForgeUiTheme.vanillaButton(tr("warforge.ui.unclaim"), 100, 20,
                    onServer(player, sp -> BlockUiActions.unclaim(sp, claim.getClaimPos()))), actionsX, y));
            y += 20 + 4;
            panel.addWidget(placed(WarForgeUiTheme.vanillaButton(tr("warforge.ui.move_citadel"), 100, 20,
                    onServer(player, BlockUiActions::moveCitadel)), actionsX, y));
        }

        int inventoryX = (width - 162) / 2;
        int inventoryY = height - 7 - 76;
        panel.addWidget(WarForgeUiTheme.inventorySlots(inventoryX, inventoryY));
        return panel;
    }

    private static WidgetGroup fob(BlockUiHolder holder, Player player) {
        final int width = 300;
        final int height = 150;
        final int bodyY = 54;
        CompoundTag state = holder.state;
        int sectionWidth = width - CONTENT_LEFT * 2;

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, width, height);
        panel.addWidget(WarForgeUiTheme.header(width));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, bodyY, sectionWidth, height - bodyY - 12));
        panel.addWidget(WarForgeUiTheme.stripe(BlockUiState.stripe(state, WarForgeUiTheme.STRIPE_DEFAULT), height));
        panel.addWidget(closeButton(width, player));
        panel.addWidget(WarForgeUiTheme.title(tr("warforge.ui.forward_operating_base")));

        if (Nbt.getBoolean(state, BlockUiState.PRESENT)) {
            if (Nbt.getBoolean(state, BlockUiState.ESTABLISHED)) {
                boolean canWarp = Nbt.getBoolean(state, BlockUiState.CAN_WARP);
                panel.addWidget(WarForgeUiTheme.subtitle(Nbt.getString(state, BlockUiState.NAME), WarForgeUiTheme.TEXT_PRIMARY, true));
                panel.addWidget(Label.text(UiLang.tr("warforge.ui.warp_tickets_d_d", Nbt.getInt(state, BlockUiState.TICKETS), Nbt.getInt(state, BlockUiState.MAX_TICKETS)),
                                canWarp ? WarForgeUiTheme.TEXT_SUCCESS : WarForgeUiTheme.TEXT_WARNING)
                        .box(sectionWidth, 9).center().at(CONTENT_LEFT, bodyY + 8));
                panel.addWidget(placed(WarForgeUiTheme.actionButton(tr("warforge.ui.warp"), 90, canWarp,
                        onServer(player, sp -> BlockUiActions.warpToFob(sp, holder.pos))), (width - 90) / 2, bodyY + 34));
            } else {
                panel.addWidget(WarForgeUiTheme.subtitle(tr("warforge.ui.enter_a_name_to_establish_this_fob"), WarForgeUiTheme.TEXT_SECONDARY, false));
                TextFieldWidget name = new TextFieldWidget(CONTENT_LEFT + 8, bodyY + 8, sectionWidth - 16, 18, null, value -> {
                });
                name.setMaxStringLength(FOB_NAME_MAX);
                name.setBackground(WarForgeUiTheme.insetBackdrop());
                panel.addWidget(name);
                panel.addWidget(placed(WarForgeUiTheme.actionButton(tr("warforge.ui.establish"), 90, click -> {
                    String value = name.getCurrentString().trim();
                    if (click.isRemote && !value.isEmpty()) {
                        PacketFobManagerAction packet = new PacketFobManagerAction();
                        packet.action = PacketFobManagerAction.Action.ESTABLISH;
                        packet.target = new DimBlockPos(holder.level.dimension(), holder.pos);
                        packet.name = value;
                        WarForgeMod.NETWORK.sendToServer(packet);
                        player.closeContainer();
                    }
                }), (width - 90) / 2, bodyY + 34));
            }
        }
        return panel;
    }

    private static WidgetGroup islandCollector(BlockUiHolder holder, Player player) {
        final int columns = 10;
        final int visibleRows = 6;
        final int bodyY = 54;
        final int listWidth = columns * SLOT_SIZE;
        final int listHeight = visibleRows * SLOT_SIZE;
        final int sectionWidth = listWidth + 10;
        final int sectionHeight = listHeight + 28;
        final int width = CONTENT_LEFT * 2 + sectionWidth;
        final int height = bodyY + sectionHeight + 12;
        CompoundTag state = holder.state;

        int inventoryY = height + 5;
        WidgetGroup root = WarForgeUiTheme.group(0, 0, width, inventoryY + WarForgeUiTheme.INVENTORY_HEIGHT);
        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, width, height);
        root.addWidget(panel);

        panel.addWidget(WarForgeUiTheme.box(0, 0, width, WarForgeUiTheme.HEADER_HEIGHT, WarForgeUiTheme.solid(WarForgeUiTheme.HEADER_FILL)));
        panel.addWidget(WarForgeUiTheme.box(CONTENT_LEFT, bodyY, sectionWidth, sectionHeight,
                new com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture(WarForgeUiTheme.SECTION_FILL)));
        panel.addWidget(WarForgeUiTheme.stripe(BlockUiState.stripe(state, WarForgeUiTheme.STRIPE_DEFAULT), height));
        panel.addWidget(closeButton(width, player));
        panel.addWidget(WarForgeUiTheme.title(tr("warforge.ui.faction_yield_storage")));

        if (Nbt.getBoolean(state, BlockUiState.PRESENT)) {
            BlockEntity be = holder.blockEntity();
            panel.addWidget(WarForgeUiTheme.subtitle(Nbt.getString(state, BlockUiState.FACTION_LABEL), Nbt.getInt(state, BlockUiState.COLOUR), true));
            panel.addWidget(Label.bold(tr("warforge.ui.collected_yield"), WarForgeUiTheme.TEXT_PRIMARY).at(CONTENT_LEFT + 5, bodyY + 5));

            int slots = Nbt.getInt(state, BlockUiState.SLOTS);
            IItemTransfer storage = be instanceof IslandCollectorBlockEntity collector
                    ? ItemTransferHelperImpl.toItemTransfer(collector.getStorageHandler())
                    : new ItemStackTransfer(slots);
            ScrollArea list = new ScrollArea(CONTENT_LEFT + 5, bodyY + 5 + 9 + 4, listWidth, listHeight);
            list.setBackground(WarForgeUiTheme.solid(WarForgeUiTheme.INSET_FILL));
            for (int i = 0; i < slots; i++) {
                list.addAt(slot(storage, i, false), (i % columns) * SLOT_SIZE, (i / columns) * SLOT_SIZE);
            }
            panel.addWidget(list.finish());
        }

        root.addWidget(WarForgeUiTheme.inventoryBox((width - WarForgeUiTheme.INVENTORY_WIDTH) / 2, inventoryY));
        return root;
    }
}
