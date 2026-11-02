package com.flansmod.warforge.compat.carryon;

import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.compat.Players;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;

/** Called by CarryOnPlacementMixin only; loads Carry On types, so Carry On must be present. */
public final class CarryOnGuard {
    private CarryOnGuard() {}

    public static @Nullable BlockState carriedState(ServerPlayer player) {
        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        return carry.isCarrying(CarryOnData.CarryType.BLOCK) ? carry.getBlock() : null;
    }

    /** Protected target: carried block drops as item(s) at the player and the carry is cleared, so Carry On places nothing. */
    public static void denyIfProtected(ServerPlayer player, BlockState state, BlockPos target) {
        ServerLevel level = Players.serverLevel(player);
        if (ProtectionsModule.mayPlace(player, level, target, state.getBlock())) {
            return;
        }
        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        BlockPos at = player.blockPosition();
        BlockEntity blockEntity = CarriedBlockEntities.of(carry, level, at);
        // Shulker boxes keep their contents inside the dropped item
        if (blockEntity instanceof Container container && !(state.getBlock() instanceof ShulkerBoxBlock)) {
            Containers.dropContents(level, at, container);
        }
        Block.dropResources(state, level, at, blockEntity, player, ItemStack.EMPTY);
        carry.clear();
        CarryOnDataManager.setCarryData(player, carry);
        player.sendSystemMessage(Component.translatable("warforge.server.carryon.denied"));
    }
}
