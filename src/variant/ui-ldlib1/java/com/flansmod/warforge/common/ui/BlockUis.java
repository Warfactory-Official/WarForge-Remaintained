package com.flansmod.warforge.common.ui;

import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.ScrollArea;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.BlockBasicClaim;
import com.flansmod.warforge.common.blocks.BlockCitadel;
import com.flansmod.warforge.common.blocks.BlockFob;
import com.flansmod.warforge.common.blocks.BlockIslandCollector;
import com.flansmod.warforge.common.blocks.TileEntityBasicClaim;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import com.flansmod.warforge.common.blocks.TileEntityFob;
import com.flansmod.warforge.common.blocks.TileEntityIslandCollector;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.UUID;
import java.util.function.Consumer;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

/**
 * The LDLib1 block UIs (citadel, basic claim, FOB, island collector), laid out like the ModularUI menus of the
 * 1.20.1 branch (ModularCitadelGui, GuiBasicClaim, GuiFob, GuiIslandCollector). The button actions are in
 * BlockUiActions.
 * <p>
 * The server captures what decides the widget tree (faction, texts, slot counts) in a state tag that the open packet
 * carries to the client, so both sides build the same tree even when the client lacks the server's faction data.
 * LDLib1 routes button presses by the widget's place in the tree, so the tree (the widgets and their order) must
 * depend only on the state tag; positions and sizes may differ. Presses run on the server with the live block entity.
 */
public final class BlockUis {
    private static final int SLOT_SIZE = 18;

    private BlockUis() {
    }

    /** Server side: the display state of the block UI at pos, or null if the block has no UI. */
    public static CompoundTag captureState(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        BlockEntity be = level.getBlockEntity(pos);
        CompoundTag tag = new CompoundTag();
        if (block instanceof BlockCitadel) {
            tag.putString("kind", "citadel");
            if (be instanceof TileEntityCitadel citadel) {
                Faction faction = WarForgeMod.FACTIONS.getFaction(citadel.getFaction());
                boolean hasFaction = faction != null;
                tag.putBoolean("present", true);
                tag.putBoolean("hasFaction", hasFaction);
                tag.putInt("stripe", citadel.colour);
                tag.putString("title", hasFaction ? citadel.getClaimDisplayName() : "Unclaimed Citadel");
                tag.putString("flagId", citadel.factionFlagId);
                tag.putBoolean("upgrades", WarForgeConfig.ENABLE_CITADEL_UPGRADES);
                tag.putInt("slots", TileEntityCitadel.NUM_YIELD_STACKS);
            }
            return tag;
        }
        if (block instanceof BlockBasicClaim) {
            tag.putString("kind", "claim");
            if (be instanceof TileEntityBasicClaim claim) {
                tag.putBoolean("present", true);
                tag.putString("title", claim.getClaimDisplayName());
                tag.putInt("slots", claim.getSlots());
            }
            return tag;
        }
        if (block instanceof BlockFob) {
            tag.putString("kind", "fob");
            if (be instanceof TileEntityFob fob) {
                Faction owner = WarForgeMod.FACTIONS.getFaction(fob.ownerFaction);
                tag.putBoolean("present", true);
                tag.putBoolean("established", !fob.ownerFaction.equals(new UUID(0, 0)) && !fob.name.isEmpty());
                tag.putString("name", fob.name);
                tag.putInt("tickets", fob.tickets);
                tag.putInt("maxTickets", fob.maxTickets);
                tag.putInt("stripe", owner != null ? owner.colour : WarForgeUiTheme.STRIPE_DEFAULT);
            }
            return tag;
        }
        if (block instanceof BlockIslandCollector) {
            tag.putString("kind", "collector");
            if (be instanceof TileEntityIslandCollector collector) {
                boolean hasFaction = !collector.getFaction().equals(Faction.nullUuid);
                tag.putBoolean("present", true);
                tag.putString("factionLabel", hasFaction ? collector.factionName : "Unclaimed");
                tag.putInt("colour", hasFaction ? collector.colour : WarForgeUiTheme.TEXT_SECONDARY);
                tag.putInt("stripe", hasFaction ? collector.colour : WarForgeUiTheme.STRIPE_DEFAULT);
                tag.putInt("slots", collector.getStorageHandler().getSlots());
            }
            return tag;
        }
        return null;
    }

    /** Builds the UI of a block (called by the holder on both sides). */
    public static ModularUI create(BlockUiHolder holder, Player player) {
        WidgetGroup root = switch (holder.state.getString("kind")) {
            case "citadel" -> citadel(holder, player);
            case "claim" -> basicClaim(holder, player);
            case "fob" -> fob(holder, player);
            case "collector" -> islandCollector(holder, player);
            default -> throw new IllegalArgumentException("No block UI for " + holder.block);
        };
        return new ModularUI(root, holder, player);
    }

    /** A button press handler that runs the action with the server player (LDLib calls it on both sides). */
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

    /** A grid of slots at x, y with the given column count. */
    private static void slotGrid(WidgetGroup parent, IItemTransfer transfer, int slots, int columns, int x, int y) {
        for (int i = 0; i < slots; i++) {
            SlotWidget slot = slot(transfer, i, true);
            slot.setSelfPosition(x + (i % columns) * SLOT_SIZE, y + (i / columns) * SLOT_SIZE);
            parent.addWidget(slot);
        }
    }

    /** A button of the action style placed at x, y. */
    private static ButtonWidget placed(ButtonWidget button, int x, int y) {
        button.setSelfPosition(x, y);
        return button;
    }

    // ---------------------------------------------------------------- citadel (ModularCitadelGui)

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

        int stripe = state.contains("stripe") ? state.getInt("stripe") : WarForgeUiTheme.STRIPE_DEFAULT;

        if (state.getBoolean("present")) {
            BlockEntity be = holder.blockEntity();
            TileEntityCitadel citadel = be instanceof TileEntityCitadel c ? c : null;
            boolean hasFaction = state.getBoolean("hasFaction");

            // Yield storage section.
            int yieldX = CONTENT_LEFT;
            panel.addWidget(WarForgeUiTheme.section(yieldX, storageY, 100, 80));
            // Flag section (the 1.20.1 backdrop is drawn 215 wide).
            int flagX = CONTENT_LEFT + 110;
            int flagWidth = 210;
            panel.addWidget(WarForgeUiTheme.section(flagX, storageY, 215, 80));
            panel.addWidget(WarForgeUiTheme.header(width));
            int actionsWidth = width - CONTENT_LEFT * 2;
            panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, actionsY, actionsWidth, 65));
            panel.addWidget(WarForgeUiTheme.stripe(stripe, height));
            panel.addWidget(closeButton(width, player));

            panel.addWidget(WarForgeUiTheme.title(state.getString("title"), hasFaction ? stripe : WarForgeUiTheme.TEXT_PRIMARY));
            panel.addWidget(WarForgeUiTheme.subtitle(hasFaction
                    ? "Faction vault, banner relay, and command center"
                    : "Claimed by the placer until a faction is founded", WarForgeUiTheme.TEXT_SECONDARY, false));

            panel.addWidget(Label.bold("Yield Storage", WarForgeUiTheme.TEXT_PRIMARY).box(90, 10)
                    .tooltip("Resources collected from claimed territory").at(yieldX + 5, storageY + 5));
            int slots = state.getInt("slots");
            slotGrid(panel, transfer(be, slots), slots, 3, yieldX + (100 - 3 * SLOT_SIZE) / 2, storageY + 20);

            // "Faction Flag" is right-anchored 130px from the section's right edge, as in the 1.20.1 menu.
            panel.addWidget(Label.bold("Faction Flag", WarForgeUiTheme.TEXT_PRIMARY).box(flagWidth - 130 - 5, 9).right()
                    .at(flagX + 5, storageY + 5));
            String flagId = state.getString("flagId");
            int flagTextY = storageY + 5 + 9 + 5;
            if (flagId.isEmpty()) {
                panel.addWidget(Label.text("Choose once. This cannot be changed later.", WarForgeUiTheme.TEXT_MUTED)
                        .wrap(flagWidth - 10).at(flagX + 5, flagTextY));
            }
            if (!flagId.isEmpty()) {
                Widget flag = holder.level.isClientSide() ? flagPreview(flagId) : new Widget(0, 0, 95, 54);
                flag.setSelfPosition(flagX + (flagWidth - 95) / 2, storageY + 20);
                panel.addWidget(flag);
            } else if (hasFaction) {
                panel.addWidget(placed(WarForgeUiTheme.actionButton("Choose", 50, onServer(player, sp -> {
                    Faction faction = citadel == null ? null : WarForgeMod.FACTIONS.getFaction(citadel.getFaction());
                    if (faction != null) {
                        BlockUiActions.chooseFlag(sp, faction.uuid);
                    }
                })), flagX + 5 + 10, flagTextY + 20 + 4));
            } else {
                panel.addWidget(Label.text("Create a faction first", 0xD6DBE0).at(180, storageY + 42));
            }

            // Command surface.
            int textX = CONTENT_LEFT + 5;
            panel.addWidget(Label.bold("Command Surface", WarForgeUiTheme.TEXT_PRIMARY).box(actionsWidth - 220 - 5, 9).right()
                    .at(textX, actionsY + 5));
            int rowX = textX + 2;
            int firstRowY = actionsY + 5 + 9 + 4 + 2;
            if (hasFaction) {
                UUID factionId = citadel == null ? Faction.nullUuid : citadel.getFaction();
                int secondRowY = firstRowY + 18 + 2 + 2;
                int[] columns = {rowX + 10, rowX + 10 + 75 + 20, rowX + 10 + (75 + 20) * 2};

                panel.addWidget(placed(WarForgeUiTheme.actionButton("Insurance", 75,
                        onServer(player, sp -> BlockUiActions.openInsurance(sp, factionId))), columns[0], firstRowY));
                panel.addWidget(placed(WarForgeUiTheme.actionButton("Faction Stats", 75,
                        onServer(player, sp -> BlockUiActions.showFactionInfo(sp, citadel.getFaction()))), columns[1], firstRowY));
                panel.addWidget(placed(WarForgeUiTheme.actionButton("Members", 75,
                        onServer(player, BlockUiActions::openMembers)), columns[2], firstRowY));

                int column = 0;
                if (state.getBoolean("upgrades")) {
                    panel.addWidget(placed(WarForgeUiTheme.actionButton("Upgrade", 75,
                            onServer(player, sp -> BlockUiActions.openUpgrade(sp, factionId))), columns[column++], secondRowY));
                }
                panel.addWidget(placed(WarForgeUiTheme.actionButton("Recolor", 75,
                        onServer(player, sp -> BlockUiActions.openRecolour(sp, citadel))), columns[column++], secondRowY));
                panel.addWidget(placed(WarForgeUiTheme.dangerButton("Disband", 75,
                        onServer(player, BlockUiActions::disband)), columns[column], secondRowY));
            } else {
                panel.addWidget(placed(WarForgeUiTheme.actionButton("Create Faction", 100,
                        onServer(player, sp -> BlockUiActions.openCreateFaction(sp, holder.pos))), rowX + 2, firstRowY + 2));
                panel.addWidget(Label.text("The placer can establish the faction from here.", WarForgeUiTheme.TEXT_MUTED)
                        .at(rowX + 2, firstRowY + 2 + 18 + 2 + 2));
            }
        } else {
            panel.addWidget(WarForgeUiTheme.header(width));
            panel.addWidget(WarForgeUiTheme.stripe(stripe, height));
            panel.addWidget(closeButton(width, player));
            panel.addWidget(WarForgeUiTheme.title("Citadel"));
        }

        root.addWidget(WarForgeUiTheme.inventoryBox((width - WarForgeUiTheme.INVENTORY_WIDTH) / 2, inventoryY));
        return root;
    }

    @OnlyIn(Dist.CLIENT)
    private static Widget flagPreview(String flagId) {
        return new com.flansmod.warforge.client.util.FlagElement(flagId, 95, 54);
    }

    // ---------------------------------------------------------------- basic claim (GuiBasicClaim)

    private static WidgetGroup basicClaim(BlockUiHolder holder, Player player) {
        final int width = 200;
        final int height = 182;
        final int gridY = 30;
        CompoundTag state = holder.state;

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, width, height);
        // The 1.20.1 claim menu has no close button (Esc closes it); kept here as this branch's menus have one.
        panel.addWidget(closeButton(width, player));

        if (state.getBoolean("present")) {
            BlockEntity be = holder.blockEntity();
            TileEntityBasicClaim claim = be instanceof TileEntityBasicClaim c ? c : null;
            panel.addWidget(Label.bold(state.getString("title"), WarForgeUiTheme.TEXT_PRIMARY).clip(width - CONTENT_LEFT - 22).at(CONTENT_LEFT, 8));
            panel.addWidget(Label.text("Yields", WarForgeUiTheme.TEXT_SECONDARY).at(CONTENT_LEFT, 20));

            int slots = state.getInt("slots");
            slotGrid(panel, transfer(be, slots), slots, 3, CONTENT_LEFT, gridY);

            int actionsX = CONTENT_LEFT + 3 * SLOT_SIZE + 12;
            int y = gridY + 2;
            panel.addWidget(placed(WarForgeUiTheme.vanillaButton("Info", 100, 20,
                    onServer(player, sp -> BlockUiActions.showFactionInfo(sp, claim.getFaction()))), actionsX, y));
            y += 20 + 4;
            panel.addWidget(placed(WarForgeUiTheme.vanillaButton("Unclaim", 100, 20,
                    onServer(player, sp -> BlockUiActions.unclaim(sp, claim.getClaimPos()))), actionsX, y));
            y += 20 + 4;
            panel.addWidget(placed(WarForgeUiTheme.vanillaButton("Move Citadel", 100, 20,
                    onServer(player, BlockUiActions::moveCitadel)), actionsX, y));
        }

        // Player inventory inside the panel, centred, 7px from the bottom (ModularPanel.bindPlayerInventory).
        int inventoryX = (width - 162) / 2;
        int inventoryY = height - 7 - 76;
        panel.addWidget(WarForgeUiTheme.inventorySlots(inventoryX, inventoryY));
        return panel;
    }

    // ---------------------------------------------------------------- FOB (GuiFob)

    private static WidgetGroup fob(BlockUiHolder holder, Player player) {
        final int width = 300;
        final int height = 150;
        final int bodyY = 54;
        CompoundTag state = holder.state;
        int sectionWidth = width - CONTENT_LEFT * 2;

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, width, height);
        panel.addWidget(WarForgeUiTheme.header(width));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, bodyY, sectionWidth, height - bodyY - 12));
        panel.addWidget(WarForgeUiTheme.stripe(state.contains("stripe") ? state.getInt("stripe") : WarForgeUiTheme.STRIPE_DEFAULT, height));
        panel.addWidget(closeButton(width, player));
        panel.addWidget(WarForgeUiTheme.title("Forward Operating Base"));

        if (state.getBoolean("present")) {
            if (state.getBoolean("established")) {
                int tickets = state.getInt("tickets");
                panel.addWidget(WarForgeUiTheme.subtitle(state.getString("name"), WarForgeUiTheme.TEXT_PRIMARY, true));
                panel.addWidget(Label.text("Warp Tickets: " + tickets + " / " + state.getInt("maxTickets"),
                                tickets > 0 ? WarForgeUiTheme.TEXT_SUCCESS : WarForgeUiTheme.TEXT_WARNING)
                        .box(sectionWidth, 9).center().at(CONTENT_LEFT, bodyY + 8));
                panel.addWidget(placed(WarForgeUiTheme.actionButton("Warp", 90,
                        onServer(player, sp -> BlockUiActions.warpToFob(sp, holder.pos))), (width - 90) / 2, bodyY + 34));
            } else {
                panel.addWidget(WarForgeUiTheme.subtitle("This FOB is not established.", WarForgeUiTheme.TEXT_SECONDARY, false));
            }
        }
        return panel;
    }

    // ---------------------------------------------------------------- island collector (GuiIslandCollector)

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

        // The 1.20.1 inventory box hangs 5px below the panel (top 202 with a 5px margin).
        int inventoryY = height + 5;
        WidgetGroup root = WarForgeUiTheme.group(0, 0, width, inventoryY + WarForgeUiTheme.INVENTORY_HEIGHT);
        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, width, height);
        root.addWidget(panel);

        panel.addWidget(WarForgeUiTheme.box(0, 0, width, WarForgeUiTheme.HEADER_HEIGHT, WarForgeUiTheme.solid(WarForgeUiTheme.HEADER_FILL)));
        panel.addWidget(WarForgeUiTheme.box(CONTENT_LEFT, bodyY, sectionWidth, sectionHeight,
                new com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture(WarForgeUiTheme.SECTION_FILL)));
        panel.addWidget(WarForgeUiTheme.stripe(state.contains("stripe") ? state.getInt("stripe") : WarForgeUiTheme.STRIPE_DEFAULT, height));
        panel.addWidget(closeButton(width, player));
        panel.addWidget(WarForgeUiTheme.title("Faction Yield Storage"));

        if (state.getBoolean("present")) {
            BlockEntity be = holder.blockEntity();
            panel.addWidget(WarForgeUiTheme.subtitle(state.getString("factionLabel"), state.getInt("colour"), true));
            panel.addWidget(Label.bold("Collected Yield", WarForgeUiTheme.TEXT_PRIMARY).at(CONTENT_LEFT + 5, bodyY + 5));

            int slots = state.getInt("slots");
            IItemTransfer storage = be instanceof TileEntityIslandCollector collector
                    ? ItemTransferHelperImpl.toItemTransfer(collector.getStorageHandler())
                    : new ItemStackTransfer(slots);
            ScrollArea list = new ScrollArea(CONTENT_LEFT + 5, bodyY + 5 + 9 + 4, listWidth, listHeight);
            list.setBackground(WarForgeUiTheme.solid(WarForgeUiTheme.INSET_FILL));
            for (int i = 0; i < slots; i++) {
                // Extract only: players may take items out but not put any in.
                list.addAt(slot(storage, i, false), (i % columns) * SLOT_SIZE, (i / columns) * SLOT_SIZE);
            }
            panel.addWidget(list.finish());
        }

        root.addWidget(WarForgeUiTheme.inventoryBox((width - WarForgeUiTheme.INVENTORY_WIDTH) / 2, inventoryY));
        return root;
    }
}
