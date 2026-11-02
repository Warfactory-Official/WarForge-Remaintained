package com.flansmod.warforge.common.ui;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.compat.Ids;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public final class BlockUiFactory extends UIFactory<BlockUiHolder> {
    public static final BlockUiFactory INSTANCE = new BlockUiFactory();

    private BlockUiFactory() {
        super(Ids.of(Tags.MODID, "block_ui"));
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        CompoundTag state = BlockUiState.capture(level, pos);
        if (state == null) {
            return;
        }
        INSTANCE.openUI(new BlockUiHolder(level, pos, level.getBlockState(pos).getBlock(), state), player);
    }

    @Override
    protected ModularUI createUITemplate(BlockUiHolder holder, Player entityPlayer) {
        return holder == null ? null : holder.createUI(entityPlayer);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected BlockUiHolder readHolderFromSyncData(FriendlyByteBuf syncData) {
        BlockPos pos = syncData.readBlockPos();
        CompoundTag state = syncData.readNbt();
        Level level = Minecraft.getInstance().level;
        if (level == null || state == null) {
            return null;
        }
        return new BlockUiHolder(level, pos, level.getBlockState(pos).getBlock(), state);
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf syncData, BlockUiHolder holder) {
        syncData.writeBlockPos(holder.pos);
        syncData.writeNbt(holder.state);
    }
}
