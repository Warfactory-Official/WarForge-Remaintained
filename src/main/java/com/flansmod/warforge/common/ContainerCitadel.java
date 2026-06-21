package com.flansmod.warforge.common;

import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import com.flansmod.warforge.server.Faction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Citadel GUI slot layout. Under modern ModularUI there is no per-GUI vanilla {@code Container};
 * the citadel {@link TileEntityCitadel} is itself the {@link net.minecraftforge.items.IItemHandlerModifiable}
 * and the slots are added directly to the {@link ModularPanel} via {@link PanelSyncManager}. The
 * 3x3 yield grid and the banner slot keep the original pixel positions; the banner slot only
 * accepts banners and shields.
 */
public final class ContainerCitadel {

    private ContainerCitadel() {
    }

    public static boolean canInteractWith(Player player, TileEntityCitadel citadel) {
        return player.level().isClientSide
                || citadel.getFaction().equals(Faction.nullUuid)
                || WarForgeMod.FACTIONS.IsPlayerInFaction(player.getUUID(), citadel.getFaction());
    }

    public static void addSlots(ModularPanel panel, PanelSyncManager syncManager, Player player, TileEntityCitadel citadel) {
        syncManager.bindPlayerInventory(player);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int index = i * 3 + j;
                panel.child(new ItemSlot()
                        .name("citadel_yield_" + index)
                        .slot(new ModularSlot(citadel, index))
                        .pos(8 + 18 * i, 32 + 18 * j));
            }
        }

        panel.child(new ItemSlot()
                .name("citadel_banner")
                .slot(new ModularSlot(citadel, TileEntityCitadel.BANNER_SLOT_INDEX)
                        .filter(ContainerCitadel::isBannerOrShield))
                .pos(152, 68));

        panel.child(SlotGroupWidget.playerInventory(false).pos(8, 100));
    }

    public static boolean isBannerOrShield(ItemStack stack) {
        return stack.getItem() instanceof BannerItem || stack.getItem() instanceof ShieldItem;
    }
}
