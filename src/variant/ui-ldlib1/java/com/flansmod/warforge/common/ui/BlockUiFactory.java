package com.flansmod.warforge.common.ui;

import com.flansmod.warforge.Tags;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * LDLib1 UI factory of the WarForge block UIs, keyed by block position. The server builds the UI and opens a
 * synchronised container; the open packet carries the position and the captured display state, from which the
 * client builds the identical widget tree. Registered once from WFUiBlock's static initialiser (both sides load the
 * blocks during registration, before any UI can open).
 */
public final class BlockUiFactory extends UIFactory<BlockUiHolder> {
    public static final BlockUiFactory INSTANCE = new BlockUiFactory();
    private static boolean registered = false;

    private BlockUiFactory() {
        super(new ResourceLocation(Tags.MODID, "block_ui"));
    }

    /** Registers the factory with LDLib (idempotent). */
    public static synchronized void register() {
        if (!registered) {
            UIFactory.register(INSTANCE);
            registered = true;
        }
    }

    /** Opens the UI of the block at pos for the player (server side). */
    public static void open(ServerPlayer player, BlockPos pos) {
        register();
        Level level = player.level();
        CompoundTag state = BlockUis.captureState(level, pos);
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
