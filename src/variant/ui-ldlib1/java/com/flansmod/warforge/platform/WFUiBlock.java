package com.flansmod.warforge.platform;

import com.flansmod.warforge.common.ui.BlockUiFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public abstract class WFUiBlock extends WFBlock {
    protected WFUiBlock(Properties properties) {
        super(properties);
    }

    public static void openUi(ServerPlayer player, BlockPos pos) {
        BlockUiFactory.open(player, pos);
    }
}
