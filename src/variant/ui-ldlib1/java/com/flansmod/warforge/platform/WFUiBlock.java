package com.flansmod.warforge.platform;

import com.flansmod.warforge.common.ui.BlockUiFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base class of blocks that open a block UI (citadel, claims, FOB, collector). The UI itself is built per UI library
 * (BlockUis); open it with {@link #openUi}.
 */
public abstract class WFUiBlock extends WFBlock {
    static {
        // The blocks are created during registration on both sides, so the LDLib1 factory is known before any
        // UI open packet can arrive.
        BlockUiFactory.register();
    }

    protected WFUiBlock(Properties properties) {
        super(properties);
    }

    /** Opens the block UI at pos for the player (server side). */
    public static void openUi(ServerPlayer player, BlockPos pos) {
        BlockUiFactory.open(player, pos);
    }
}
