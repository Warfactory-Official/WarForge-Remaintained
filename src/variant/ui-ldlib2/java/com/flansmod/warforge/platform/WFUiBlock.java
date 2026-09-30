package com.flansmod.warforge.platform;

import com.flansmod.warforge.common.ui.BlockUis;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base class of blocks that open a block UI (citadel, claims, FOB, collector). The UI itself is built per UI library
 * (BlockUis); open it with {@link #openUi}.
 */
public abstract class WFUiBlock extends WFBlock implements BlockUIMenuType.BlockUI {
    protected WFUiBlock(Properties properties) {
        super(properties);
    }

    /** Opens the block UI at pos for the player (server side). */
    public static void openUi(ServerPlayer player, BlockPos pos) {
        BlockUIMenuType.openUI(player, pos);
    }

    @Override
    public final ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return BlockUis.create(this, holder);
    }
}
