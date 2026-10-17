package com.flansmod.warforge.common.ui;

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
import com.flansmod.warforge.common.network.PacketEstablishFob;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.Faction;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.UUID;
import java.util.function.Consumer;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

/**
 * The LDLib2 block UIs (citadel, basic claim, FOB, island collector), laid out like the ModularUI block menus of
 * the 1.20.1 build (ModularCitadelGui, GuiBasicClaim, GuiFob, GuiIslandCollector). The button actions are in
 * BlockUiActions.
 *
 * <p>The UIs are built on both sides; the element tree is the same on both (client-only painters are replaced by
 * empty elements on the server).
 */
public final class BlockUis {
    private static final int SLOT_SIZE = 18;

    private BlockUis() {
    }

    /** Builds the UI of a block (called by WFUiBlock on both sides). */
    public static ModularUI create(Block block, BlockUIMenuType.BlockUIHolder holder) {
        if (block instanceof BlockCitadel) return citadel(holder);
        if (block instanceof BlockBasicClaim) return basicClaim(holder);
        if (block instanceof BlockFob) return fob(holder);
        if (block instanceof BlockIslandCollector) return islandCollector(holder);
        throw new IllegalArgumentException("No block UI for " + block);
    }

    /** Runs the action with the holder's player if it is the server player. */
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

    // ------------------------------------------------------------------------------------------------ citadel

    private static final int CITADEL_WIDTH = 350;
    private static final int CITADEL_HEIGHT = 214;
    private static final int CITADEL_STORAGE_Y = 50;
    private static final int CITADEL_ACTIONS_Y = 140;

    private static ModularUI citadel(BlockUIMenuType.BlockUIHolder holder) {
        boolean client = isClient(holder);
        BlockEntity be = holder.player.level().getBlockEntity(holder.pos);

        UIElement root = screenRoot();
        UIElement panel = panel(CITADEL_WIDTH, CITADEL_HEIGHT, client);
        root.addChild(panel);

        if (be instanceof TileEntityCitadel citadel) {
            Faction faction = WarForgeMod.FACTIONS.getFaction(citadel.getFaction());
            boolean hasFaction = faction != null;
            int stripe = hasFaction ? citadel.colour : NO_FACTION_STRIPE;

            // Yield storage (left)
            UIElement yieldSection = place(panel, box(100, 80, sectionBackdrop()), CONTENT_LEFT, CITADEL_STORAGE_Y, 100, 80);
            place(yieldSection, tooltip(boldText("Yield Storage", TEXT_PRIMARY), "Resources collected from claimed territory"), 5, 5, 90, 10);
            UIElement grid = place(yieldSection, new UIElement(), (100 - 3 * SLOT_SIZE) / 2f, 20, 3 * SLOT_SIZE, 3 * SLOT_SIZE);
            grid.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(dev.vfyjxf.taffy.style.FlexWrap.WRAP));
            for (int i = 0; i < TileEntityCitadel.NUM_YIELD_STACKS; i++) {
                grid.addChild(slot(new ItemSlot().bind(citadel, i)));
            }

            // Faction flag (right): flow 210 wide over a 215 wide backdrop
            int flagX = CONTENT_LEFT + 110;
            place(panel, box(215, 80, sectionBackdrop()), flagX, CITADEL_STORAGE_Y, 215, 80);
            UIElement flagSection = section(210, 80);
            flagSection.style(s -> s.background(IGuiTexture.EMPTY));
            place(panel, flagSection, flagX, CITADEL_STORAGE_Y, 210, 80);
            Label flagTitle = fromEnd(fit(text("Faction Flag", TEXT_PRIMARY)), 130, 5);
            flagTitle.layout(l -> l.marginBottom(5));
            flagSection.addChild(flagTitle);
            if (!citadel.factionFlagId.isEmpty()) {
                place(flagSection, flagPreview(citadel.factionFlagId, client), (210 - 95) / 2f, 20, 95, 54);
            } else {
                flagSection.addChild(wrap(text("Choose once. This cannot be changed later.", TEXT_MUTED), 210, client));
                if (hasFaction) {
                    UUID factionId = faction.uuid;
                    flagSection.addChild(serverButton("Choose", 50, holder, sp -> BlockUiActions.chooseFlag(sp, factionId)));
                } else {
                    place(panel, fit(text("Create a faction first", 0xD6DBE0)), 180, CITADEL_STORAGE_Y + 42);
                }
            }

            headerBackdrop(panel, CITADEL_WIDTH);

            // Command surface (bottom)
            int actionsWidth = CITADEL_WIDTH - CONTENT_LEFT * 2;
            UIElement actions = section(panel, CONTENT_LEFT, CITADEL_ACTIONS_Y, actionsWidth, 65);
            Label actionsTitle = fromEnd(fit(text("Command Surface", TEXT_PRIMARY)), 220, 5);
            actionsTitle.layout(l -> l.marginBottom(4));
            actions.addChild(actionsTitle);
            if (hasFaction) {
                UUID factionId = faction.uuid;
                int rowX = 5 + 2 + 10;
                int[] columns = {rowX, rowX + 95, rowX + 190};
                place(actions, serverButton("Insurance", 75, holder, sp -> BlockUiActions.openInsurance(sp, factionId)),
                        columns[0], 19, 75, BUTTON_HEIGHT);
                place(actions, serverButton("Faction Stats", 75, holder, sp -> BlockUiActions.showFactionInfo(sp, citadel.getFaction())),
                        columns[1], 19, 75, BUTTON_HEIGHT);
                place(actions, serverButton("Members", 75, holder, BlockUiActions::openMembers),
                        columns[2], 19, 75, BUTTON_HEIGHT);
                int column = 0;
                if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
                    place(actions, serverButton("Upgrade", 75, holder, sp -> BlockUiActions.openUpgrade(sp, factionId)),
                            columns[column++], 41, 75, BUTTON_HEIGHT);
                }
                place(actions, serverButton("Recolor", 75, holder, sp -> BlockUiActions.openRecolour(sp, citadel)),
                        columns[column++], 41, 75, BUTTON_HEIGHT);
                Button disband = dangerButton("Disband", 75, null);
                disband.setOnServerClick(event -> onServer(holder, BlockUiActions::disband));
                place(actions, disband, columns[column], 41, 75, BUTTON_HEIGHT);
            } else {
                UIElement column = flow(FlexDirection.COLUMN);
                column.layout(l -> l.width(actionsWidth - 10).marginTop(2));
                Button create = serverButton("Create Faction", 100, holder, sp -> BlockUiActions.openCreateFaction(sp, holder.pos));
                create.layout(l -> l.marginAll(2));
                column.addChild(create);
                Label placer = fit(text("The placer can establish the faction from here.", TEXT_MUTED));
                placer.layout(l -> l.marginAll(2));
                column.addChild(placer);
                actions.addChild(column);
            }

            colorStripe(panel, stripe, CITADEL_HEIGHT);
            closeButton(panel, CITADEL_WIDTH, holder);

            String title = hasFaction ? citadel.getClaimDisplayName() : "Unclaimed Citadel";
            String subtitle = hasFaction ? "Faction vault, banner relay, and command center"
                    : "Claimed by the placer until a faction is founded";
            title(panel, title, hasFaction ? citadel.colour : TEXT_PRIMARY);
            subtitle(panel, text(subtitle, TEXT_SECONDARY));
        } else {
            headerBackdrop(panel, CITADEL_WIDTH);
            closeButton(panel, CITADEL_WIDTH, holder);
        }

        root.addChild(inventoryPanel(client, INVENTORY_GAP));
        return ModularUI.of(UI.of(root), holder.player);
    }

    private static UIElement flagPreview(String flagId, boolean client) {
        return client ? clientFlag(flagId) : new UIElement();
    }

    @OnlyIn(Dist.CLIENT)
    private static UIElement clientFlag(String flagId) {
        return new com.flansmod.warforge.client.util.FlagElement(flagId);
    }

    // ------------------------------------------------------------------------------------------------ basic claim

    private static final int CLAIM_WIDTH = 200;
    private static final int CLAIM_HEIGHT = 182;
    private static final int CLAIM_GRID_Y = 30;

    private static ModularUI basicClaim(BlockUIMenuType.BlockUIHolder holder) {
        boolean client = isClient(holder);
        UIElement root = screenRoot();
        UIElement panel = panel(CLAIM_WIDTH, CLAIM_HEIGHT, client);
        root.addChild(panel);

        if (holder.player.level().getBlockEntity(holder.pos) instanceof TileEntityBasicClaim claim) {
            place(panel, boldText(claim.getClaimDisplayName(), TEXT_PRIMARY), CONTENT_LEFT, 8, CLAIM_WIDTH - CONTENT_LEFT * 2 - 18, 9);
            place(panel, text("Yields", TEXT_SECONDARY), CONTENT_LEFT, 20, 60, 9);

            int rows = (claim.getSlots() + 2) / 3;
            UIElement grid = place(panel, new UIElement(), CONTENT_LEFT, CLAIM_GRID_Y, 3 * SLOT_SIZE, rows * SLOT_SIZE);
            grid.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(dev.vfyjxf.taffy.style.FlexWrap.WRAP));
            for (int i = 0; i < claim.getSlots(); i++) {
                grid.addChild(slot(new ItemSlot().bind(claim, i)));
            }

            int actionsX = CONTENT_LEFT + 3 * SLOT_SIZE + 12;
            Button info = vanillaButton("Info", 100, 20);
            info.setOnServerClick(event -> onServer(holder, sp -> BlockUiActions.showFactionInfo(sp, claim.getFaction())));
            place(panel, info, actionsX, CLAIM_GRID_Y + 2, 100, 20);
            Button unclaim = vanillaButton("Unclaim", 100, 20);
            unclaim.setOnServerClick(event -> onServer(holder, sp -> BlockUiActions.unclaim(sp, claim.getClaimPos())));
            place(panel, unclaim, actionsX, CLAIM_GRID_Y + 26, 100, 20);
            Button moveCitadel = vanillaButton("Move Citadel", 100, 20);
            moveCitadel.setOnServerClick(event -> onServer(holder, BlockUiActions::moveCitadel));
            place(panel, moveCitadel, actionsX, CLAIM_GRID_Y + 50, 100, 20);
        }

        com.flansmod.warforge.api.modularui.WarForgeUiTheme.closeButton(panel, CLAIM_WIDTH)
                .setOnServerClick(event -> onServer(holder, BlockUiActions::close));
        // The player inventory sits at the bottom of the panel (bindPlayerInventory on 1.20.1).
        place(panel, vanillaSlots(new InventorySlots()), (CLAIM_WIDTH - 162) / 2f, CLAIM_HEIGHT - 7 - 77);
        return ModularUI.of(UI.of(root), holder.player);
    }

    // ------------------------------------------------------------------------------------------------ FOB

    private static final int FOB_WIDTH = 300;
    private static final int FOB_HEIGHT = 150;

    private static ModularUI fob(BlockUIMenuType.BlockUIHolder holder) {
        boolean client = isClient(holder);
        int sectionWidth = FOB_WIDTH - CONTENT_LEFT * 2;

        UIElement root = screenRoot();
        UIElement panel = panel(FOB_WIDTH, FOB_HEIGHT, client);
        root.addChild(panel);

        BlockEntity be = holder.player.level().getBlockEntity(holder.pos);
        TileEntityFob fob = be instanceof TileEntityFob f ? f : null;
        Faction owner = fob == null ? null : WarForgeMod.FACTIONS.getFaction(fob.ownerFaction);

        headerBackdrop(panel, FOB_WIDTH);
        place(panel, box(sectionWidth, FOB_HEIGHT - BODY_Y - 12, sectionBackdrop()),
                CONTENT_LEFT, BODY_Y, sectionWidth, FOB_HEIGHT - BODY_Y - 12);
        colorStripe(panel, owner != null ? owner.colour : NO_FACTION_STRIPE, FOB_HEIGHT);
        closeButton(panel, FOB_WIDTH, holder);
        title(panel, "Forward Operating Base", TEXT_PRIMARY);

        if (fob != null) {
            boolean established = !fob.ownerFaction.equals(new UUID(0, 0)) && !fob.name.isEmpty();
            float buttonX = (FOB_WIDTH - 90) / 2f;
            if (established) {
                subtitle(panel, text(fob.name, TEXT_PRIMARY));
                Label tickets = text("Warp Tickets: " + fob.tickets + " / " + fob.maxTickets,
                        fob.tickets > 0 ? TEXT_SUCCESS : TEXT_WARNING);
                tickets.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER));
                place(panel, tickets, CONTENT_LEFT, BODY_Y + 8, sectionWidth, 9);
                Button warp = actionButton("Warp", 90, fob.tickets > 0);
                if (fob.tickets > 0) {
                    warp.setOnServerClick(event -> onServer(holder, sp -> BlockUiActions.warpToFob(sp, holder.pos)));
                }
                place(panel, warp, buttonX, BODY_Y + 34, 90, BUTTON_HEIGHT);
            } else {
                subtitle(panel, text("Enter a name to establish this FOB.", TEXT_SECONDARY));
                TextField name = new TextField().setText("").setAnyString();
                name.style(s -> s.background(insetBackdrop()));
                place(panel, name, CONTENT_LEFT, BODY_Y + 8, sectionWidth - 16, 18);
                Button establish = actionButton("Establish", 90, true);
                establish.setOnClick(event -> {
                    String value = name.getValue().trim();
                    if (value.isEmpty()) {
                        return;
                    }
                    PacketEstablishFob packet = new PacketEstablishFob();
                    packet.mPos = new DimBlockPos(holder.player.level().dimension(), holder.pos);
                    packet.mName = value;
                    WarForgeMod.NETWORK.sendToServer(packet);
                    holder.player.closeContainer();
                });
                place(panel, establish, buttonX, BODY_Y + 34, 90, BUTTON_HEIGHT);
            }
        }

        return ModularUI.of(UI.of(root), holder.player);
    }

    // ------------------------------------------------------------------------------------------------ island collector

    private static final int COLLECTOR_COLUMNS = 10;
    private static final int COLLECTOR_VISIBLE_ROWS = 6;
    private static final int COLLECTOR_LIST_WIDTH = COLLECTOR_COLUMNS * SLOT_SIZE;
    private static final int COLLECTOR_LIST_HEIGHT = COLLECTOR_VISIBLE_ROWS * SLOT_SIZE;
    private static final int COLLECTOR_SECTION_WIDTH = COLLECTOR_LIST_WIDTH + 10;
    private static final int COLLECTOR_SECTION_HEIGHT = COLLECTOR_LIST_HEIGHT + 28;
    private static final int COLLECTOR_WIDTH = CONTENT_LEFT * 2 + COLLECTOR_SECTION_WIDTH;
    private static final int COLLECTOR_HEIGHT = BODY_Y + COLLECTOR_SECTION_HEIGHT + 12;

    private static ModularUI islandCollector(BlockUIMenuType.BlockUIHolder holder) {
        boolean client = isClient(holder);

        // Panel centred alone, inventory hung below it: spacer of the inventory's height above.
        UIElement root = screenRoot();
        root.addChild(box(1, INVENTORY_BOX_HEIGHT));
        UIElement panel = panel(COLLECTOR_WIDTH, COLLECTOR_HEIGHT, client);
        root.addChild(panel);

        BlockEntity be = holder.player.level().getBlockEntity(holder.pos);
        TileEntityIslandCollector collector = be instanceof TileEntityIslandCollector c ? c : null;
        boolean hasFaction = collector != null && !collector.getFaction().equals(Faction.nullUuid);

        place(panel, box(COLLECTOR_WIDTH, HEADER_HEIGHT, fill(HEADER_FILL)), 0, 0, COLLECTOR_WIDTH, HEADER_HEIGHT);
        UIElement storage = place(panel, section(COLLECTOR_SECTION_WIDTH, COLLECTOR_SECTION_HEIGHT),
                CONTENT_LEFT, BODY_Y, COLLECTOR_SECTION_WIDTH, COLLECTOR_SECTION_HEIGHT);
        storage.style(s -> s.background(fill(SECTION_FILL)));
        colorStripe(panel, hasFaction ? collector.colour : NO_FACTION_STRIPE, COLLECTOR_HEIGHT);
        closeButton(panel, COLLECTOR_WIDTH, holder);

        title(panel, "Faction Yield Storage", TEXT_PRIMARY);
        subtitle(panel, text(hasFaction ? collector.factionName : "Unclaimed", hasFaction ? collector.colour : TEXT_SECONDARY));

        Label heading = fit(text("Collected Yield", TEXT_PRIMARY));
        heading.layout(l -> l.marginBottom(4));
        storage.addChild(heading);

        if (collector != null) {
            IItemHandlerModifiable handler = collector.getStorageHandler();
            int slots = handler.getSlots();
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
