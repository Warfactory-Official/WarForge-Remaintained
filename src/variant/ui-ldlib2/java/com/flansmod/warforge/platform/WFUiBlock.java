package com.flansmod.warforge.platform;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketBlockUiState;
import com.flansmod.warforge.common.ui.BlockUiState;
import com.flansmod.warforge.common.ui.BlockUis;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public abstract class WFUiBlock extends WFBlock implements BlockUIMenuType.BlockUI {
    protected WFUiBlock(Properties properties) {
        super(properties);
    }

    public static void openUi(ServerPlayer player, BlockPos pos) {
        PacketBlockUiState packet = new PacketBlockUiState();
        packet.pos = pos;
        packet.state = BlockUiState.capture(player.level(), pos);
        WarForgeMod.NETWORK.sendTo(packet, player);
        BlockUIMenuType.openUI(player, pos);
    }

    @Override
    public final ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return BlockUis.create(this, holder);
    }
}
