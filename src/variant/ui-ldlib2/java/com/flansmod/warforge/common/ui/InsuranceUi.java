package com.flansmod.warforge.common.ui;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketInsuranceData;
import com.flansmod.warforge.common.util.FactionInsuranceItemHandler;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

/** Insurance stash menu (GuiFactionInsurance of 1.20.1): deposit-only slots, leader voids per slot. */
public final class InsuranceUi {
    private static final String PATH = "insurance";
    private static final int WIDTH = 356;
    private static final int HEIGHT = 180;
    private static final int CELL_SIZE = 18;
    private static final int LIST_HEIGHT = 56;

    /** Server: holder for the {@link PlayerUIMenuType#openUI} call in progress (it resolves synchronously). */
    @Nullable
    private static PlayerUIMenuType.PlayerUIHolder opening;

    private InsuranceUi() {
    }

    public static void register() {
        PlayerUIMenuType.register(Ids.of(Tags.MODID, PATH), player -> player.level().isClientSide() ? clientHolder() : opening);
    }

    /** Server: view packet first => client {@link PacketInsuranceData#latest} set before the menu opens. */
    public static void open(ServerPlayer player, PacketInsuranceData view, @Nullable Faction faction) {
        WarForgeMod.NETWORK.sendTo(view, player);
        IItemHandlerModifiable stash = faction == null ? new ItemStackHandler(0) : new FactionInsuranceItemHandler(faction);
        opening = holder(view, stash, faction);
        try {
            PlayerUIMenuType.openUI(player, Ids.of(Tags.MODID, PATH));
        } finally {
            opening = null;
        }
    }

    private static PlayerUIMenuType.PlayerUIHolder clientHolder() {
        PacketInsuranceData view = PacketInsuranceData.latest;
        return holder(view, new ItemStackHandler(view.slotCount) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !WarForgeConfig.isInsuranceBlacklisted(stack);
            }
        }, null);
    }

    private static PlayerUIMenuType.PlayerUIHolder holder(PacketInsuranceData view, IItemHandlerModifiable stash, @Nullable Faction faction) {
        return new PlayerUIMenuType.PlayerUIHolder() {
            @Override
            public ModularUI createUI(Player player) {
                return build(player, view, stash, faction);
            }

            @Override
            public boolean isStillValid(Player player) {
                return faction == null || WarForgeMod.FACTIONS.getFaction(faction.uuid) == faction;
            }
        };
    }

    /** {@code faction} only on the server (void target). */
    private static ModularUI build(Player player, PacketInsuranceData view, IItemHandlerModifiable stash, @Nullable Faction faction) {
        boolean client = player.level().isClientSide();
        int sectionWidth = WIDTH - CONTENT_LEFT * 2;

        UIElement root = screenRoot();
        UIElement panel = panel(WIDTH, HEIGHT, client);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        UIElement body = section(panel, CONTENT_LEFT, BODY_Y, sectionWidth, 118);
        colorStripe(panel, view.hasFaction ? view.factionColor : NO_FACTION_STRIPE, HEIGHT);
        closeButton(panel, WIDTH, client).setOnServerClick(event -> {
            if (player instanceof ServerPlayer sp) BlockUiActions.close(sp);
        });

        title(panel, "Insurance Stash", TEXT_PRIMARY);
        subtitle(panel, text(view.hasFaction ? view.factionName : "No faction selected",
                view.hasFaction ? view.factionColor : TEXT_SECONDARY));

        if (!view.hasFaction) {
            Label empty = fit(text("No insurance stash is available.", TEXT_INFO));
            empty.layout(l -> l.marginVertical(6));
            body.addChild(empty);
            return ModularUI.of(UI.of(root), player);
        }

        Label heading = fit(text("Protected Reserve", TEXT_PRIMARY));
        heading.layout(l -> l.marginBottom(4));
        body.addChild(heading);
        Label depositInfo = wrap(text(view.canDeposit
                ? "Officers and leaders may deposit. Stored items cannot be withdrawn."
                : "Only officers and leaders may deposit into the stash.", TEXT_MUTED), sectionWidth, client);
        depositInfo.layout(l -> l.marginBottom(2));
        body.addChild(depositInfo);
        Label voidInfo = wrap(text(view.canVoid
                        ? "Leader actions: use the red X to permanently void a slot."
                        : "Only the leader can void stored items to free space.",
                view.canVoid ? 0xFFCC88 : TEXT_MUTED), sectionWidth, client);
        voidInfo.layout(l -> l.marginBottom(4));
        body.addChild(voidInfo);

        int listWidth = sectionWidth - 10;
        int columns = Math.max(1, (listWidth - 6) / CELL_SIZE);
        ScrollerView list = insetList(listWidth, LIST_HEIGHT);
        for (int first = 0; first < view.slotCount; first += columns) {
            UIElement row = row(listWidth, CELL_SIZE);
            for (int slot = first; slot < Math.min(view.slotCount, first + columns); slot++) {
                row.addChild(cell(view, stash, faction, slot));
            }
            list.addScrollViewChild(row);
        }
        body.addChild(list);

        root.addChild(inventoryPanel(client, INVENTORY_GAP));
        return ModularUI.of(UI.of(root), player);
    }

    private static UIElement cell(PacketInsuranceData view, IItemHandlerModifiable stash, @Nullable Faction faction, int slot) {
        UIElement cell = box(CELL_SIZE, CELL_SIZE);
        ItemSlot itemSlot = vanillaSlots(new ItemSlot().bind(new SlotItemHandler(stash, slot, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return view.canDeposit && super.mayPlace(stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        }));
        place(cell, itemSlot, 0, 0, CELL_SIZE, CELL_SIZE);
        if (view.canVoid) {
            Button voidButton = dangerButton("x", 8, 8, null);
            voidButton.setOnServerClick(event -> {
                if (faction != null) faction.setInsuranceStack(slot, ItemStack.EMPTY);
            });
            tooltip(voidButton, "Click to void the slot", "Warning, this cannot be undone");
            place(cell, voidButton, 10, 0, 8, 8);
        }
        return cell;
    }
}
