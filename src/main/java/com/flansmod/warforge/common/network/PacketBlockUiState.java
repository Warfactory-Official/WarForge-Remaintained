package com.flansmod.warforge.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Server to client: the state of the block UI opening next (sent before the menu opens). */
public class PacketBlockUiState extends PacketBase {
    public BlockPos pos = BlockPos.ZERO;
    public CompoundTag state = new CompoundTag();

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeBlockPos(pos);
        data.writeNbt(state);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        pos = data.readBlockPos();
        state = data.readNbt();
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        PacketSecurity.violation(player, "Received PacketBlockUiState on server side");
    }
}
