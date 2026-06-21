package com.flansmod.warforge.common;

import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import com.flansmod.warforge.common.blocks.TileEntityBasicClaim;
import com.flansmod.warforge.server.Faction;
import net.minecraft.world.entity.player.Player;

/**
 * Basic-claim GUI slot layout. Under modern ModularUI there is no per-GUI vanilla {@code Container};
 * the claim {@link TileEntityBasicClaim} is itself the
 * {@link net.minecraftforge.items.IItemHandlerModifiable} and the slots are added directly to the
 * {@link ModularPanel} via {@link PanelSyncManager}. The 3x3 yield grid keeps the original pixel
 * positions.
 */
public final class ContainerBasicClaim {

    private ContainerBasicClaim() {
    }

    public static boolean canInteractWith(Player player, TileEntityBasicClaim claim) {
        return player.level().isClientSide
                || claim.getFaction().equals(Faction.nullUuid)
                || WarForgeMod.FACTIONS.IsPlayerInFaction(player.getUUID(), claim.getFaction());
    }

    public static void addSlots(ModularPanel panel, PanelSyncManager syncManager, Player player, TileEntityBasicClaim claim) {
        syncManager.bindPlayerInventory(player);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int index = i * 3 + j;
                panel.child(new ItemSlot()
                        .name("claim_yield_" + index)
                        .slot(new ModularSlot(claim, index))
                        .pos(8 + 18 * i, 32 + 18 * j));
            }
        }

        panel.child(SlotGroupWidget.playerInventory(false).pos(8, 100));
    }
}
