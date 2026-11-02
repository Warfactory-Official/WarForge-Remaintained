package com.flansmod.warforge.compat.carryon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import tschipp.carryon.common.carry.CarryOnData;

final class CarriedBlockEntities {
    private CarriedBlockEntities() {}

    static @Nullable BlockEntity of(CarryOnData carry, ServerLevel level, BlockPos pos) {
        return carry.getBlockEntity(pos);
    }
}
