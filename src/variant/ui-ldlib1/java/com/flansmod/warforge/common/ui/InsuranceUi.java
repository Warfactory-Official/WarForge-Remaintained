package com.flansmod.warforge.common.ui;

import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.ScrollArea;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.common.network.PacketInsuranceData;
import com.flansmod.warforge.common.util.FactionInsuranceItemHandler;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

public final class InsuranceUi extends UIFactory<InsuranceUi.Holder> {
    static final InsuranceUi INSTANCE = new InsuranceUi();
    private static final int WIDTH = 356;
    private static final int HEIGHT = 180;
    private static final int BODY_Y = 54;
    private static final int SECTION_WIDTH = WIDTH - CONTENT_LEFT * 2;
    private static final int CELL_SIZE = 18;
    private static final int LIST_HEIGHT = 56;

    private InsuranceUi() {
        super(Ids.of(Tags.MODID, "insurance"));
    }

    public static void open(ServerPlayer player, PacketInsuranceData view, @Nullable Faction faction) {
        IItemHandlerModifiable stash = faction == null ? new ItemStackHandler(0) : new FactionInsuranceItemHandler(faction);
        INSTANCE.openUI(new Holder(view, stash, faction, false), player);
    }

    @Override
    protected ModularUI createUITemplate(Holder holder, Player player) {
        return new ModularUI(build(holder, player), holder, player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected Holder readHolderFromSyncData(FriendlyByteBuf syncData) {
        PacketInsuranceData view = new PacketInsuranceData();
        view.decodeInto(syncData);
        return new Holder(view, new ItemStackHandler(view.slotCount) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !FactionInsuranceItemHandler.isBlacklisted(stack);
            }
        }, null, true);
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf syncData, Holder holder) {
        holder.view.encodeInto(syncData);
    }

    public record Holder(PacketInsuranceData view, IItemHandlerModifiable stash, @Nullable Faction faction,
                         boolean isRemote) implements IUIHolder {
        @Override
        public ModularUI createUI(Player player) {
            return INSTANCE.createUITemplate(this, player);
        }

        @Override
        public boolean isInvalid() {
            return faction != null && WarForgeMod.FACTIONS.registry.getFaction(faction.getUuid()) != faction;
        }

        @Override
        public void markAsDirty() {
        }
    }

    private static WidgetGroup build(Holder holder, Player player) {
        PacketInsuranceData view = holder.view;
        int inventoryY = HEIGHT + WarForgeUiTheme.INVENTORY_GAP;
        WidgetGroup root = WarForgeUiTheme.group(0, 0, WIDTH,
                view.hasFaction ? inventoryY + WarForgeUiTheme.INVENTORY_HEIGHT : HEIGHT);
        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, WIDTH, HEIGHT);
        root.addWidget(panel);

        panel.addWidget(WarForgeUiTheme.header(WIDTH));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, BODY_Y, SECTION_WIDTH, 118));
        panel.addWidget(WarForgeUiTheme.stripe(view.hasFaction ? view.factionColor : WarForgeUiTheme.STRIPE_DEFAULT, HEIGHT));
        panel.addWidget(WarForgeUiTheme.closeButton(WIDTH, click -> {
            if (!click.isRemote && player instanceof ServerPlayer sp) BlockUiActions.close(sp);
        }));
        panel.addWidget(WarForgeUiTheme.title(tr("warforge.ui.insurance_stash")));
        panel.addWidget(view.hasFaction
                ? WarForgeUiTheme.subtitle(view.factionName, view.factionColor, true)
                : WarForgeUiTheme.subtitle(tr("warforge.ui.no_faction_selected"), WarForgeUiTheme.TEXT_SECONDARY, true));

        int textX = CONTENT_LEFT + 5;
        if (!view.hasFaction) {
            panel.addWidget(Label.text(tr("warforge.ui.no_insurance_stash_is_available"), WarForgeUiTheme.TEXT_NOTE).at(textX, BODY_Y + 5 + 6));
            return root;
        }

        int y = BODY_Y + 5;
        panel.addWidget(Label.bold(tr("warforge.ui.protected_reserve"), WarForgeUiTheme.TEXT_PRIMARY).at(textX, y));
        y += 9 + 4;
        panel.addWidget(Label.text(view.canDeposit
                        ? tr("warforge.ui.officers_and_leaders_may_deposit_stored_items_ca")
                        : tr("warforge.ui.only_officers_and_leaders_may_deposit_into_the_s"),
                WarForgeUiTheme.TEXT_MUTED).clip(SECTION_WIDTH - 10).at(textX, y));
        y += 9 + 2;
        panel.addWidget(Label.text(view.canVoid
                        ? tr("warforge.ui.leader_actions_use_the_red_x_to_permanently_void")
                        : tr("warforge.ui.only_the_leader_can_void_stored_items_to_free_sp"),
                view.canVoid ? WarForgeUiTheme.TEXT_LEADER_ACTION : WarForgeUiTheme.TEXT_MUTED).clip(SECTION_WIDTH - 10).at(textX, y));
        y += 9 + 4;

        int listWidth = SECTION_WIDTH - 10;
        int columns = Math.max(1, listWidth / CELL_SIZE);
        IItemTransfer stash = ItemTransferHelperImpl.toItemTransfer(holder.stash);
        ScrollArea list = new ScrollArea(textX, y, listWidth, LIST_HEIGHT);
        for (int slot = 0; slot < view.slotCount; slot++) {
            int cellX = (slot % columns) * CELL_SIZE;
            int cellY = (slot / columns) * CELL_SIZE;
            list.addAt(new SlotWidget(stash, slot, 0, 0, false, view.canDeposit), cellX, cellY);
            if (view.canVoid) {
                int captured = slot;
                ButtonWidget voidButton = WarForgeUiTheme.smallDangerButton("x", 8, 8, click -> {
                    if (!click.isRemote && holder.faction != null) holder.faction.setInsuranceStack(captured, ItemStack.EMPTY);
                });
                voidButton.setHoverTooltips(tr("warforge.ui.click_to_void_the_slot"), tr("warforge.ui.warning_this_cannot_be_undone"));
                list.addAt(voidButton, cellX + 10, cellY);
            }
        }
        panel.addWidget(list.finish());

        root.addWidget(WarForgeUiTheme.inventoryBox((WIDTH - WarForgeUiTheme.INVENTORY_WIDTH) / 2, inventoryY));
        return root;
    }
}
